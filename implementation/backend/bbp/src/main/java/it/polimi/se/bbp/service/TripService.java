package it.polimi.se.bbp.service;

import it.polimi.se.bbp.dto.request.TripSearchRequest;
import it.polimi.se.bbp.geo.Coordinate;
import it.polimi.se.bbp.dto.result.CyclingRouteResult;
import it.polimi.se.bbp.dto.result.GeocodeResult;
import it.polimi.se.bbp.dto.request.TripManualRecordRequest;
import it.polimi.se.bbp.entity.MeteorologicalData;
import it.polimi.se.bbp.entity.Trip;
import it.polimi.se.bbp.entity.TripPoint;
import it.polimi.se.bbp.entity.User;
import it.polimi.se.bbp.mapper.entity.TripMapper;
import it.polimi.se.bbp.mapper.entity.TripPointMapper;
import it.polimi.se.bbp.repository.TripPointRepository;
import it.polimi.se.bbp.repository.TripRepository;
import it.polimi.se.bbp.service.mapbox.MapboxService;
import it.polimi.se.bbp.service.openmeteo.OpenMeteoService;
import it.polimi.se.bbp.specification.TripSpecification;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service for trip operations.
 * Manages creation, deletion, and retrieval using Mapbox for geocoding/routing.
 * Optionally enriches trips with meteorological data from Open-Meteo API.
 */
@Service
@RequiredArgsConstructor
public class TripService {

    /**
     * Repository for trip data access.
     */
    private final TripRepository tripRepository;

    /**
     * Repository for trip point data access.
     */
    private final TripPointRepository tripPointRepository;

    /**
     * Service for user authentication and retrieval.
     */
    private final UserAuthService userAuthService;

    /**
     * Service for Mapbox API interactions (geocoding and routing).
     */
    private final MapboxService mapboxService;

    /**
     * Service for Open-Meteo API interactions (weather data).
     */
    private final OpenMeteoService openMeteoService;

    /**
     * Mapper for converting request data to Trip entities.
     */
    private final TripMapper tripMapper;

    /**
     * Mapper for converting route coordinates to TripPoint entities.
     */
    private final TripPointMapper tripPointMapper;

    /**
     * Maximum page size to prevent excessive memory usage.
     */
    private static final int MAX_PAGE_SIZE = 100;

    /**
     * Creates new trip from manual user input using batch insert.
     * Workflow: (1) authenticate user, (2) geocode addresses, (3) calculate cycling route,
     * (4) create trip entity with metrics, (5) batch save trip/points, (6) optionally
     * enrich with weather data, (7) reload complete entity.
     * @param request manual trip recording request with addresses, times, and optional max speed
     * @return created trip entity with all points and optional meteorological data
     */
    @Transactional
    public Trip recordTripManually(TripManualRecordRequest request) {
        User user = userAuthService.getAuthenticatedUser();
        List<GeocodeResult> geocodeResults = mapboxService.geocodeAddresses(request.addresses());
        CyclingRouteResult routeResult = calculateCyclingRoute(geocodeResults);
        Trip trip = createTripEntity(request, user, geocodeResults, routeResult);
        saveTripData(trip, routeResult);
        return tripRepository.save(trip);
    }

    /**
     * Deletes trip by ID.
     * Verifies ownership before deletion.
     * @param tripId ID of trip to delete
     * @throws EntityNotFoundException if trip not found
     * @throws AccessDeniedException if user is not owner
     */
    @Transactional
    public void deleteTrip(Long tripId) {
        User user = userAuthService.getAuthenticatedUser();
        Trip trip = tripRepository.findById(tripId).orElseThrow(() -> new EntityNotFoundException("Trip not found"));
        if (!trip.getRecordedBy().getId().equals(user.getId()))
            throw new AccessDeniedException("You can only delete your own trips");
        tripRepository.delete(trip);
    }

    /**
     * Retrieves trip by ID with all relationships loaded.
     * Only accessible to trip owner (requires authentication).
     * Loads trip points and meteorological data (if available).
     * @param tripId ID of trip to retrieve
     * @return trip with points and meteorological data loaded
     * @throws EntityNotFoundException if trip not found
     * @throws AccessDeniedException if user is not trip owner
     */
    @Transactional(readOnly = true)
    public Trip getTripById(Long tripId) {
        User user = userAuthService.getAuthenticatedUser();
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new EntityNotFoundException("Trip not found"));
        // Verify ownership
        if (!trip.getRecordedBy().getId().equals(user.getId()))
            throw new AccessDeniedException("You can only view your own trips");
        // Eagerly load relationships after permission check
        Hibernate.initialize(trip.getTripPoints());
        Hibernate.initialize(trip.getMeteorologicalData());
        return trip;
    }

    /**
     * Retrieves paginated list of trips for authenticated user.
     * Returns trips with all relationships loaded (points and weather data if available).
     * Default sorting: startTime DESC (newest first).
     * @param page page number (0-indexed)
     * @param size trips per page (positive, max MAX_PAGE_SIZE)
     * @param sortBy field name to sort by (default: startTime)
     * @param direction sort direction ASC or DESC (default: DESC)
     * @return page of trips with relationships loaded
     * @throws IllegalArgumentException if pagination parameters invalid
     */
    @Transactional(readOnly = true)
    public Page<Trip> getUserTrips(int page, int size, String sortBy, String direction) {
        User user = userAuthService.getAuthenticatedUser();
        return fetchAndEnrichTrips(page, size, sortBy, direction,
                pageable -> tripRepository.findPageByRecordedByIdWithWeather(user.getId(), pageable));
    }

    /**
     * Searches trips for authenticated user based on filters.
     * All filters optional and combinable. Returns paginated list with all relationships loaded.
     * Default sorting: startTime DESC (newest first).
     * Available filters: originSearch/destinationSearch (text), startTimeFrom/startTimeTo (date range),
     * minDistance/maxDistance (km), minDuration/maxDuration (minutes), hasWeatherData (boolean).
     * @param searchRequest search request with filter criteria
     * @param page page number (0-indexed)
     * @param size trips per page (positive, max MAX_PAGE_SIZE)
     * @param sortBy field name to sort by (default: startTime)
     * @param direction sort direction ASC or DESC (default: DESC)
     * @return page of trips matching criteria with relationships loaded
     * @throws IllegalArgumentException if pagination parameters invalid
     */
    @Transactional(readOnly = true)
    public Page<Trip> searchTrips(TripSearchRequest searchRequest, int page, int size, String sortBy, String direction) {
        User user = userAuthService.getAuthenticatedUser();
        TripSpecification specification = new TripSpecification(user.getId(), searchRequest);
        return fetchAndEnrichTrips(page, size, sortBy, direction,
                pageable -> tripRepository.findAll(specification, pageable));
    }

    /**
     * Template method for fetching and enriching trips with common pagination workflow.
     * Eliminates duplication by extracting common setup, enrichment, and return logic.
     * Actual query delegated to provided function for flexibility.
     * @param page page number (0-indexed)
     * @param size page size
     * @param sortBy field to sort by
     * @param direction sort direction (ASC or DESC)
     * @param queryFunction function executing actual query given Pageable
     * @return page of trips with relationships loaded and enriched
     */
    private Page<Trip> fetchAndEnrichTrips(int page, int size, String sortBy, String direction,
                                           Function<Pageable, Page<Trip>> queryFunction) {
        validatePaginationParameters(page, size);
        Pageable pageable = createPageable(page, size, sortBy, direction);
        Page<Trip> tripPage = queryFunction.apply(pageable);
        if (!tripPage.isEmpty())
            enrichTripsWithPoints(tripPage);
        return tripPage;
    }

    /**
     * Calculates cycling route between multiple waypoints.
     * Extracts coordinates from geocode results and calls Mapbox routing.
     * @param geocodeResults list of geocoded addresses with coordinates
     * @return calculated cycling route with distance and coordinates
     */
    private CyclingRouteResult calculateCyclingRoute(List<GeocodeResult> geocodeResults) {
        List<Coordinate> waypoints = GeocodeResult.extractCoordinates(geocodeResults);
        return mapboxService.calculateCyclingRoute(waypoints);
    }

    /**
     * Creates Trip entity with all calculated metrics.
     * Computes duration, distance, average speed, and validates max speed.
     * @param request trip request with user input data
     * @param user authenticated user recording trip
     * @param geocodeResults geocoded addresses
     * @param routeResult calculated cycling route
     * @return Trip entity ready to save
     */
    private Trip createTripEntity(TripManualRecordRequest request, User user, List<GeocodeResult> geocodeResults, CyclingRouteResult routeResult) {
        GeocodeResult origin = geocodeResults.getFirst();
        GeocodeResult destination = geocodeResults.getLast();
        // Calculate metrics
        int totalDurationMinutes = calculateDuration(request);
        BigDecimal totalDistanceKm = calculateDistance(routeResult);
        BigDecimal averageSpeed = calculateAverageSpeed(totalDistanceKm, totalDurationMinutes);
        BigDecimal maxSpeed = validateMaxSpeed(request.maxSpeed(), averageSpeed);
        return tripMapper.toEntity(request, user, origin, destination, totalDurationMinutes, totalDistanceKm, averageSpeed, maxSpeed);
    }

    /**
     * Calculates trip duration in minutes.
     * @param request trip request with start and end times
     * @return duration in minutes
     */
    private int calculateDuration(TripManualRecordRequest request) {
        return (int) Duration.between(request.startTime(), request.endTime()).toMinutes();
    }

    /**
     * Calculates total distance in kilometers from route result.
     * Converts meters to kilometers, rounded to 3 decimal places.
     * @param routeResult calculated cycling route
     * @return total distance in kilometers
     */
    private BigDecimal calculateDistance(CyclingRouteResult routeResult) {
        return BigDecimal.valueOf(routeResult.distanceInMeters() / 1000.0).setScale(3, RoundingMode.HALF_UP);
    }

    /**
     * Calculates average speed in km/h.
     * Returns zero if duration is zero or negative.
     * @param totalDistanceKm total distance in kilometers
     * @param totalDurationMinutes total duration in minutes
     * @return average speed in km/h, rounded to 2 decimal places
     */
    private BigDecimal calculateAverageSpeed(BigDecimal totalDistanceKm, int totalDurationMinutes) {
        if (totalDurationMinutes <= 0)
            return BigDecimal.ZERO;
        return totalDistanceKm.divide(BigDecimal.valueOf(totalDurationMinutes / 60.0), 2, RoundingMode.HALF_UP);
    }

    /**
     * Validates and adjusts maximum speed to be greater than or equal to average speed.
     * Returns null if maxSpeed not provided, or averageSpeed if maxSpeed too low.
     * @param requestMaxSpeed max speed from user request (nullable)
     * @param averageSpeed calculated average speed
     * @return validated maximum speed or null
     */
    private BigDecimal validateMaxSpeed(BigDecimal requestMaxSpeed, BigDecimal averageSpeed) {
        if (requestMaxSpeed == null)
            return null;
        if (requestMaxSpeed.compareTo(averageSpeed) < 0)
            return averageSpeed;
        return requestMaxSpeed;
    }

    /**
     * Saves trip and associated data with batch insert.
     * Attempts to enrich with meteorological data from Open-Meteo API.
     * If weather data retrieval fails, trip saved without meteorological data.
     * @param trip trip entity to save
     * @param routeResult route result with coordinates for trip points
     */
    private void saveTripData(Trip trip, CyclingRouteResult routeResult) {
        tripRepository.save(trip);
        // Batch insert trip points
        List<TripPoint> tripPoints = tripPointMapper.toEntities(routeResult.routeCoordinates(), trip, null);
        tripPointRepository.saveAll(tripPoints);
        trip.setTripPoints(tripPoints);
        // Attempt to fetch weather data
        try {
            MeteorologicalData weatherData = openMeteoService.getWeatherData(
                    trip.getOriginLatitude(),
                    trip.getOriginLongitude(),
                    trip.getStartTime(),
                    trip
            );
            trip.setMeteorologicalData(weatherData);
        } catch (Exception e) {
            // Weather data retrieval failed - trip saved without it
        }
    }

    /**
     * Validates pagination parameters are within acceptable ranges.
     * @param page page number (must be non-negative)
     * @param size page size (must be positive and not exceed MAX_PAGE_SIZE)
     * @throws IllegalArgumentException if parameters invalid
     */
    private void validatePaginationParameters(int page, int size) {
        if (page < 0)
            throw new IllegalArgumentException("Page number must be non-negative");
        if (size <= 0)
            throw new IllegalArgumentException("Page size must be positive");
        if (size > MAX_PAGE_SIZE)
            throw new IllegalArgumentException("Page size must not exceed " + MAX_PAGE_SIZE);
    }

    /**
     * Creates Pageable object with pagination and sorting parameters.
     * Defaults to DESC if direction not ASC.
     * @param page page number
     * @param size page size
     * @param sortBy field to sort by
     * @param direction sort direction (ASC or DESC)
     * @return configured Pageable object
     */
    private Pageable createPageable(int page, int size, String sortBy, String direction) {
        Sort.Direction sortDirection = "ASC".equalsIgnoreCase(direction)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
    }

    /**
     * Enriches page of trips by batch loading trip points.
     * Fetches all points in single query to avoid N+1 problem.
     * Groups by trip ID and assigns to corresponding trips.
     * @param tripPage page of trips to enrich
     */
    private void enrichTripsWithPoints(Page<Trip> tripPage) {
        List<Long> tripIds = tripPage.getContent().stream()
                .map(Trip::getId)
                .toList();
        // Batch fetch all points
        List<TripPoint> allPoints = tripPointRepository.findAllByTripIdInOrderByTripIdAscSequentialPositionAsc(tripIds);
        // Group by trip ID
        Map<Long, List<TripPoint>> pointsByTripId = allPoints.stream()
                .collect(Collectors.groupingBy(
                        point -> point.getTrip().getId(),
                        Collectors.toList()
                ));
        // Assign to trips
        tripPage.forEach(trip -> {
            List<TripPoint> points = pointsByTripId.getOrDefault(trip.getId(), new ArrayList<>());
            trip.setTripPoints(points);
        });
    }

}