package it.polimi.se.bbp.controller;

import it.polimi.se.bbp.dto.request.TripManualRecordRequest;
import it.polimi.se.bbp.dto.response.PagedTripResponse;
import it.polimi.se.bbp.dto.response.TripResponse;
import it.polimi.se.bbp.entity.Trip;
import it.polimi.se.bbp.mapper.response.PagedTripResponseMapper;
import it.polimi.se.bbp.mapper.response.TripResponseMapper;
import it.polimi.se.bbp.service.TripService;
import jakarta.validation.Valid;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for trip management endpoints.
 * Handles trip creation, deletion, and retrieval operations.
 * All endpoints require authentication via JWT token.
 */
@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    /**
     * Service for handling trip operations.
     */
    private final TripService tripService;

    /**
     * Mapper for converting Trip entities to TripResponse DTOs.
     */
    private final TripResponseMapper tripResponseMapper;

    /**
     * Mapper for converting paginated Trip results to PagedTripResponse DTOs.
     */
    private final PagedTripResponseMapper pagedTripResponseMapper;

    /**
     * Retrieves a paginated list of trips for the authenticated user.
     * Returns trips with all associated data including trip points and meteorological data.
     * Supports pagination and sorting to efficiently handle large datasets.
     * Query parameters:
     * - page: Page number (0-indexed, default: 0)
     * - size: Number of trips per page (default: 20, max: 100)
     * - sortBy: Field to sort by (default: startTime, examples: endTime, totalDistance, averageSpeed)
     * - direction: Sort direction (default: DESC, values: ASC or DESC)
     * Example requests:
     * - GET /api/trips (default: page 0, size 20, sort by startTime DESC)
     * - GET /api/trips?page=1&size=10 (second page with 10 items)
     * - GET /api/trips?page=0&size=50&sortBy=totalDistance&direction=ASC (first page, 50 items, sorted by distance ascending)
     * Response includes:
     * - content: List of trips with all details
     * - currentPage: Current page number
     * - pageSize: Number of items per page
     * - totalElements: Total number of trips
     * - totalPages: Total number of pages
     * - hasNext: Whether there is a next page
     * - hasPrevious: Whether there is a previous page
     * - firstPage: Whether this is the first page
     * - lastPage: Whether this is the last page
     * @param page the page number (0-indexed, default: 0)
     * @param size the number of trips per page (default: 20, max: 100)
     * @param sortBy the field to sort by (default: startTime)
     * @param direction the sort direction: ASC or DESC (default: DESC)
     * @return ResponseEntity with HTTP 200 OK status and paginated trip response
     * @throws IllegalArgumentException if pagination parameters are invalid
     */
    @GetMapping
    public ResponseEntity<PagedTripResponse> getUserTrips(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "startTime") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction
    ) {
        // Get paginated trips from service
        Page<Trip> tripPage = tripService.getUserTrips(page, size, sortBy, direction);
        // Map to PagedTripResponse DTO
        PagedTripResponse response = pagedTripResponseMapper.toPagedResponse(tripPage);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * Creates a new trip from manual user input.
     * Geocodes addresses, calculates cycling route, and stores trip with GPS coordinates.
     * Optionally enriches the trip with meteorological data if available.
     * The trip is associated with the authenticated user.
     * Workflow:
     * 1. Geocode all provided addresses in parallel
     * 2. Calculate optimal cycling route through waypoints
     * 3. Compute trip metrics (distance, speed, duration)
     * 4. Save trip with batch-inserted route points
     * 5. Attempt to fetch weather data (non-blocking, optional)
     * 6. Return complete trip data with all relationships
     * @param request the manual trip recording request containing addresses and trip details
     * @return ResponseEntity with HTTP 201 CREATED status and trip response
     * @throws IllegalArgumentException if addresses are invalid or route cannot be calculated
     * @throws IllegalStateException if Mapbox service is unavailable
     */
    @PostMapping("/manual")
    public ResponseEntity<TripResponse> createTripManually(@Valid @RequestBody TripManualRecordRequest request) {
        Trip trip = tripService.recordTripManual(request);
        TripResponse response = tripResponseMapper.toResponse(trip);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Deletes a trip by ID.
     * Only the user who created the trip can delete it.
     * All associated data (trip points, meteorological data) are automatically deleted via cascade.
     * @param id the ID of the trip to delete
     * @return ResponseEntity with HTTP 204 NO CONTENT status
     * @throws EntityNotFoundException if trip not found
     * @throws AccessDeniedException if user is not the owner
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTrip(@PathVariable Long id) {
        tripService.deleteTrip(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}