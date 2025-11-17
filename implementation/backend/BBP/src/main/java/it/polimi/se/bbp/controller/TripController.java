package it.polimi.se.bbp.controller;

import it.polimi.se.bbp.dto.request.TripManualRecordRequest;
import it.polimi.se.bbp.dto.response.TripResponse;
import it.polimi.se.bbp.entity.Trip;
import it.polimi.se.bbp.mapper.response.TripResponseMapper;
import it.polimi.se.bbp.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller for trip management endpoints.
 * Handles trip creation, deletion, and retrieval operations.
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
     * Retrieves all trips for the authenticated user.
     * @return ResponseEntity with HTTP 200 OK status and list of trip responses
     */
    @GetMapping
    public ResponseEntity<List<TripResponse>> getUserTrips() {
        List<Trip> trips = tripService.getUserTrips();
        List<TripResponse> responses = trips.stream().map(tripResponseMapper::toResponse).collect(Collectors.toList());
        return ResponseEntity.status(HttpStatus.OK).body(responses);
    }

    /**
     * Creates a new trip from manual user input.
     * Geocodes addresses, calculates cycling route, and stores trip with GPS coordinates.
     * @param request the manual trip recording request containing addresses and trip details
     * @return ResponseEntity with HTTP 201 CREATED status and trip response
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
     * @param id the ID of the trip to delete
     * @return ResponseEntity with HTTP 204 NO CONTENT status
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTrip(@PathVariable Long id) {
        tripService.deleteTrip(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}