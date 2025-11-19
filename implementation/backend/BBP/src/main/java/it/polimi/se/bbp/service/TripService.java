package it.polimi.se.bbp.service;

import it.polimi.se.bbp.dto.mapbox.Coordinate;
import it.polimi.se.bbp.dto.mapbox.CyclingRouteResult;
import it.polimi.se.bbp.dto.mapbox.GeocodeResult;
import it.polimi.se.bbp.dto.request.TripManualRecordRequest;
import it.polimi.se.bbp.entity.MeteorologicalData;
import it.polimi.se.bbp.entity.Trip;
import it.polimi.se.bbp.entity.TripPoint;
import it.polimi.se.bbp.entity.User;
import it.polimi.se.bbp.mapper.entity.TripMapper;
import it.polimi.se.bbp.mapper.entity.TripPointMapper;
import it.polimi.se.bbp.repository.TripPointRepository;
import it.polimi.se.bbp.repository.TripRepository;
import it.polimi.se.bbp.repository.UserRepository;
import it.polimi.se.bbp.service.mapbox.MapboxService;
import it.polimi.se.bbp.service.openmeteo.OpenMeteoService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for handling trip operations.
 * Manages trip creation, deletion, and retrieval using Mapbox APIs for geocoding and routing.
 * Optionally enriches trips with meteorological data from Open-Meteo API.
 */
@Service
@RequiredArgsConstructor
public class TripService {

    /**
     * Repository for trip data access operations.
     */
    private final TripRepository tripRepository;

    /**
     * Repository for trip point data access operations.
     */
    private final TripPointRepository tripPointRepository;

    /**
     * Repository for user data access operations.
     */
    private final UserRepository userRepository;

    /**
     * Service for interacting with Mapbox APIs (geocoding and routing).
     */
    private final MapboxService mapboxService;

    /**
     * Service for interacting with Open-Meteo API (weather data).
     */
    private final OpenMeteoService openMeteoService;

    /**
     * Mapper for converting trip request data to Trip entities.
     */
    private final TripMapper tripMapper;

    /**
     * Mapper for converting route coordinates to TripPoint entities.
     */
    private final TripPointMapper tripPointMapper;

    /**
     * Entity manager for JPA operations.
     */
    private final EntityManager entityManager;

    /**
     * Creates a new trip from manual user input using BATCH INSERT.
     * OPTIMIZED: Uses batch insert for TripPoints to improve performance.
     * Workflow:
     * 1. Geocode addresses and calculate cycling route
     * 2. Calculate trip metrics (distance, speed, duration)
     * 3. Save trip entity first (without points)
     * 4. BATCH INSERT trip points
     * 5. Optionally enrich with meteorological data if available
     * 6. Reload complete entity with all relationships
     * @param request the manual trip recording request
     * @return the created trip entity with optional meteorological data
     * @throws IllegalArgumentException if addresses are invalid or route cannot be calculated
     * @throws IllegalStateException if Mapbox service is unavailable
     */
    @Transactional
    public Trip recordTripManual(TripManualRecordRequest request) {
        // Get authenticated user ID from security context
        Long userId = getCurrentUserId();
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalStateException("Authenticated user not found in database"));
        // Step 1: Geocode all addresses to get coordinates
        List<GeocodeResult> geocodeResults = geocodeAddresses(request.getAddresses());
        // Step 2: Extract coordinates as waypoints for route calculation
        List<Coordinate> waypoints = extractCoordinates(geocodeResults);
        // Step 3: Calculate cycling route through all waypoints
        CyclingRouteResult routeResult = mapboxService.calculateCyclingRoute(waypoints);
        // Step 4: Extract origin and destination from geocode results
        GeocodeResult origin = geocodeResults.getFirst();
        GeocodeResult destination = geocodeResults.getLast();
        // Step 5: Calculate trip metrics (business logic)
        int totalDurationMinutes = calculateDuration(request);
        BigDecimal totalDistanceKm = calculateDistance(routeResult);
        BigDecimal averageSpeed = calculateAverageSpeed(totalDistanceKm, totalDurationMinutes);
        BigDecimal maxSpeed = validateMaxSpeed(request.getMaxSpeed(), averageSpeed);
        // Step 6: Create and save Trip entity FIRST (without points)
        Trip trip = tripMapper.toEntity(request, user, origin, destination, totalDurationMinutes, totalDistanceKm, averageSpeed, maxSpeed);
        trip = tripRepository.save(trip);
        // Step 7: BATCH INSERT - Create and save TripPoint entities
        List<TripPoint> tripPoints = tripPointMapper.toEntities(routeResult.getRouteCoordinates(), trip, null);
        tripPointRepository.saveAll(tripPoints);
        entityManager.flush();
        // Step 8: Try to fetch and associate meteorological data (optional, non-blocking)
        try {
            MeteorologicalData weatherData = openMeteoService.getWeatherData(
                    trip.getOriginLatitude(),
                    trip.getOriginLongitude(),
                    trip.getStartTime(),
                    trip
            );
            // Set weather data - will be saved automatically by dirty checking
            trip.setMeteorologicalData(weatherData);
            entityManager.flush();
        } catch (IllegalArgumentException e) {
            // Weather data not available (trip too old, no data for location, etc.)
            // Trip is saved without meteorological data
        } catch (IllegalStateException e) {
            // Weather service unavailable (API down, rate limit, network issues)
            // Trip is saved without meteorological data
        } catch (Exception e) {
            // Unexpected error - trip is saved without meteorological data
        }
        entityManager.clear();
        // Step 9: Reload complete entity with all relationships (points + weather data)
        trip = tripRepository.findByIdWithPointsAndWeather(trip.getId()).orElseThrow(() -> new IllegalStateException("Trip not found after save"));
        // Return trip (with or without meteorological data)
        return trip;
    }

    /**
     * Deletes a trip by ID.
     * Verifies that the trip belongs to the authenticated user before deletion.
     * @param tripId the ID of the trip to delete
     * @throws EntityNotFoundException if trip is not found
     * @throws AccessDeniedException if user is not the owner of the trip
     */
    @Transactional
    public void deleteTrip(Long tripId) {
        Long userId = getCurrentUserId();
        Trip trip = tripRepository.findById(tripId).orElseThrow(() -> new EntityNotFoundException("Trip not found"));
        // Verify ownership
        if (!trip.getRecordedBy().getId().equals(userId))
            throw new AccessDeniedException("You can only delete your own trips");
        tripRepository.delete(trip);
    }

    /**
     * Retrieves a paginated list of trips for the authenticated user.
     * OPTIMIZED: Returns only the requested page of trips with all relationships eagerly loaded.
     * Each trip includes all trip points and meteorological data if available.
     * Supports sorting by any Trip field (e.g., startTime, totalDistance, averageSpeed).
     * Default sorting: startTime DESC (newest first).
     * @param page the page number (0-indexed, first page is 0)
     * @param size the number of trips per page (must be positive)
     * @param sortBy the field name to sort by (default: startTime)
     * @param direction the sort direction: ASC or DESC (default: DESC)
     * @return page of trips with all relationships loaded
     * @throws IllegalArgumentException if page or size parameters are invalid
     */
    public Page<Trip> getUserTrips(int page, int size, String sortBy, String direction) {
        // Get authenticated user ID
        Long userId = getCurrentUserId();
        // Validate pagination parameters
        validatePaginationParameters(page, size);
        // Create Pageable with sorting
        Pageable pageable = createPageable(page, size, sortBy, direction);
        // Execute paginated query with eager loading
        return tripRepository.findPageByRecordedByIdWithPointsAndWeather(userId, pageable);
    }

    /**
     * Validates pagination parameters to ensure they are within acceptable ranges.
     * @param page the page number (must be non-negative)
     * @param size the page size (must be positive)
     * @throws IllegalArgumentException if parameters are invalid
     */
    private void validatePaginationParameters(int page, int size) {
        if (page < 0)
            throw new IllegalArgumentException("Page number must be non-negative");
        if (size <= 0)
            throw new IllegalArgumentException("Page size must be positive");
    }

    /**
     * Creates a Pageable object with the specified pagination and sorting parameters.
     * Converts sort direction string to Sort.Direction enum.
     * @param page the page number
     * @param size the page size
     * @param sortBy the field to sort by
     * @param direction the sort direction (ASC or DESC)
     * @return configured Pageable object
     */
    private Pageable createPageable(int page, int size, String sortBy, String direction) {
        // Parse sort direction (default to DESC if invalid)
        Sort.Direction sortDirection = "ASC".equalsIgnoreCase(direction)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        // Create and return Pageable
        return PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
    }

    /**
     * Geocodes a list of addresses using Mapbox API in parallel.
     * Uses the parallel geocoding method for improved performance.
     * @param addresses list of address strings
     * @return list of geocode results with formatted addresses and coordinates
     */
    private List<GeocodeResult> geocodeAddresses(List<String> addresses) {
        return mapboxService.geocodeAddressesParallel(addresses);
    }

    /**
     * Extracts coordinates from geocode results to use as waypoints.
     * @param geocodeResults list of geocode results
     * @return list of coordinates
     */
    private List<Coordinate> extractCoordinates(List<GeocodeResult> geocodeResults) {
        List<Coordinate> coordinates = new ArrayList<>();
        for (GeocodeResult result : geocodeResults) {
            coordinates.add(result.getCoordinate());
        }
        return coordinates;
    }

    /**
     * Calculates the trip duration in minutes.
     * @param request the trip request containing start and end times
     * @return duration in minutes
     */
    private int calculateDuration(TripManualRecordRequest request) {
        return (int) Duration.between(request.getStartTime(), request.getEndTime()).toMinutes();
    }

    /**
     * Calculates the total distance in kilometers from the route result.
     * @param routeResult the calculated cycling route
     * @return total distance in kilometers
     */
    private BigDecimal calculateDistance(CyclingRouteResult routeResult) {
        return BigDecimal.valueOf(routeResult.getDistanceInMeters() / 1000.0).setScale(3, RoundingMode.HALF_UP);
    }

    /**
     * Calculates the average speed in km/h.
     * @param totalDistanceKm total distance in kilometers
     * @param totalDurationMinutes total duration in minutes
     * @return average speed in km/h
     */
    private BigDecimal calculateAverageSpeed(BigDecimal totalDistanceKm, int totalDurationMinutes) {
        return totalDistanceKm.divide(BigDecimal.valueOf(totalDurationMinutes / 60.0), 2, RoundingMode.HALF_UP);
    }

    /**
     * Validates and adjusts the maximum speed to ensure it's greater than or equal to average speed.
     * If maxSpeed is less than averageSpeed, it's set to averageSpeed.
     * @param requestMaxSpeed the max speed from the user request (may be null)
     * @param averageSpeed the calculated average speed
     * @return validated maximum speed
     */
    private BigDecimal validateMaxSpeed(BigDecimal requestMaxSpeed, BigDecimal averageSpeed) {
        // If max speed is not provided, set it to null
        if (requestMaxSpeed == null)
            return null;
        // If max speed is less than average speed, correct it to average speed
        if (requestMaxSpeed.compareTo(averageSpeed) < 0)
            return averageSpeed;
        // Otherwise, use the provided max speed
        return requestMaxSpeed;
    }

    /**
     * Retrieves the authenticated user's ID from the security context.
     * @return the user ID
     */
    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

}