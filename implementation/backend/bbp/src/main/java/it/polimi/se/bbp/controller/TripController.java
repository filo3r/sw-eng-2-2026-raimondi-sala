package it.polimi.se.bbp.controller;

import it.polimi.se.bbp.dto.request.TripManualRecordRequest;
import it.polimi.se.bbp.dto.request.TripSearchRequest;
import it.polimi.se.bbp.dto.response.PagedTripResponse;
import it.polimi.se.bbp.dto.response.TripResponse;
import it.polimi.se.bbp.entity.Trip;
import it.polimi.se.bbp.mapper.response.PagedTripResponseMapper;
import it.polimi.se.bbp.mapper.response.TripResponseMapper;
import it.polimi.se.bbp.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for trip management.
 * Handles creation, deletion, and retrieval of user trips.
 * All endpoints require JWT authentication.
 */
@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    /**
     * Service for trip business logic.
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
     * Retrieves paginated trips for the authenticated user.
     * Returns trips with all associated data including trip points and meteorological data.
     * Supports sorting by: startTime (default), endTime, totalDistance, averageSpeed.
     * @param page page number, 0-indexed (default: 0)
     * @param size number of items per page (default: 20, max: 100)
     * @param sortBy field to sort by (default: startTime)
     * @param direction sort direction ASC or DESC (default: DESC)
     * @return paginated trip response with navigation metadata
     */
    @GetMapping
    public ResponseEntity<PagedTripResponse> getUserTrips(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "startTime") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction
    ) {
        Page<Trip> tripPage = tripService.getUserTrips(page, size, sortBy, direction);
        PagedTripResponse response = pagedTripResponseMapper.toPagedResponse(tripPage);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * Searches trips for the authenticated user with optional filters.
     * All filters are optional and can be combined to narrow results.
     * Supports filtering by origin, destination, and start time range.
     * Empty filters return all trips, equivalent to GET /api/trips.
     * @param searchRequest filter criteria including origin, destination, and time range (all optional)
     * @param page page number, 0-indexed (default: 0)
     * @param size number of items per page (default: 20, max: 100)
     * @param sortBy field to sort by (default: startTime)
     * @param direction sort direction ASC or DESC (default: DESC)
     * @return paginated search results matching filters
     */
    @PostMapping("/search")
    public ResponseEntity<PagedTripResponse> searchTrips(
            @Valid @RequestBody TripSearchRequest searchRequest,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "startTime") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction
    ) {
        Page<Trip> tripPage = tripService.searchTrips(searchRequest, page, size, sortBy, direction);
        PagedTripResponse response = pagedTripResponseMapper.toPagedResponse(tripPage);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * Creates a new trip from manual user input.
     * Geocodes addresses, calculates cycling route, computes metrics, and stores trip with GPS coordinates.
     * Optionally enriches trip with meteorological data if available (non-blocking).
     * Workflow: parallel geocoding, route calculation, metrics computation,
     * batch insert of route points, optional weather data fetch.
     * @param request manual trip recording data including addresses and trip details
     * @return created trip with all relationships
     */
    @PostMapping("/manual")
    public ResponseEntity<TripResponse> createTripManually(@Valid @RequestBody TripManualRecordRequest request) {
        Trip trip = tripService.recordTripManually(request);
        TripResponse response = tripResponseMapper.toResponse(trip);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Deletes a trip by ID.
     * Only the trip owner can delete it.
     * All associated data (points, meteorological data) are cascade deleted.
     * This operation is irreversible.
     * @param id trip ID to delete
     * @return empty response with 204 status
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTrip(@PathVariable Long id) {
        tripService.deleteTrip(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}