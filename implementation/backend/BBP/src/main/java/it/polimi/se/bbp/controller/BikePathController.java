package it.polimi.se.bbp.controller;

import it.polimi.se.bbp.dto.request.BikePathManualCreateRequest;
import it.polimi.se.bbp.dto.request.BikePathUpdateRequest;
import it.polimi.se.bbp.dto.response.BikePathResponse;
import it.polimi.se.bbp.entity.BikePath;
import it.polimi.se.bbp.mapper.response.BikePathResponseMapper;
import it.polimi.se.bbp.service.BikePathService;
import jakarta.validation.Valid;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller for bike path management endpoints.
 * Handles bike path creation, updates, deletion, and retrieval operations.
 * All endpoints require authentication via JWT token.
 */
@RestController
@RequestMapping("/api/bike-paths")
@RequiredArgsConstructor
public class BikePathController {

    /**
     * Service for handling bike path operations.
     */
    private final BikePathService bikePathService;

    /**
     * Mapper for converting BikePath entities to BikePathResponse DTOs.
     */
    private final BikePathResponseMapper bikePathResponseMapper;

    /**
     * Retrieves all bike paths created by the authenticated user.
     * Returns both published and private bike paths belonging to the user.
     * @return ResponseEntity with HTTP 200 OK status and list of bike path responses
     */
    @GetMapping
    public ResponseEntity<List<BikePathResponse>> getUserBikePaths() {
        List<BikePath> bikePaths = bikePathService.getUserBikePaths();
        List<BikePathResponse> responses = bikePaths.stream()
                .map(bikePathResponseMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.status(HttpStatus.OK).body(responses);
    }

    /**
     * Creates a new bike path from user input.
     * Geocodes addresses, calculates cycling route, validates obstacles proximity,
     * and stores bike path with GPS coordinates and obstacles.
     * The bike path is created with the specified status and published flag.
     * If published is true, the bike path will be visible to all users and can be updated by anyone.
     * If published is false, the bike path is private and only visible/editable by the creator.
     * Obstacles are validated to ensure they are within the acceptable distance from the route.
     * @param request the bike path creation request containing addresses, status, obstacles, and metadata
     * @return ResponseEntity with HTTP 201 CREATED status and bike path response
     * @throws IllegalArgumentException if addresses are invalid, route cannot be calculated, or obstacles are too far
     * @throws IllegalStateException if Mapbox service is unavailable
     */
    @PostMapping("/manual")
    public ResponseEntity<BikePathResponse> createBikePathManually(@Valid @RequestBody BikePathManualCreateRequest request) {
        BikePath bikePath = bikePathService.createBikePathManual(request);
        BikePathResponse response = bikePathResponseMapper.toResponse(bikePath);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates an existing bike path.
     * Supports partial updates - only provided fields will be modified.
     * Permission rules:
     * - The creator can always update their bike path (regardless of published status)
     * - Other users can only update published bike paths
     * - Private bike paths can only be updated by their creator
     * Updatable fields:
     * - status: Change the condition/maintenance status
     * - published: Change visibility (public/private)
     * - obstaclesToAdd: Add new obstacles to the bike path
     * - obstaclesToUpdate: Modify existing obstacles (type, severity, active status)
     * New obstacles are geocoded and validated against the route buffer.
     * The bike path score is recalculated after updates.
     * @param id the ID of the bike path to update
     * @param request the update request containing fields to modify
     * @return ResponseEntity with HTTP 200 OK status and updated bike path response
     * @throws EntityNotFoundException if bike path not found
     * @throws AccessDeniedException if user lacks permission to update
     * @throws IllegalArgumentException if obstacle data is invalid or obstacle ID not found
     * @throws IllegalStateException if Mapbox service is unavailable (for new obstacles)
     */
    @PatchMapping("/{id}")
    public ResponseEntity<BikePathResponse> updateBikePath(@PathVariable Long id, @Valid @RequestBody BikePathUpdateRequest request) {
        BikePath bikePath = bikePathService.updateBikePath(id, request);
        BikePathResponse response = bikePathResponseMapper.toResponse(bikePath);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * Deletes a bike path by ID.
     * Only the user who created the bike path can delete it.
     * All associated data (bike path points, obstacles) are automatically deleted via cascade.
     * @param id the ID of the bike path to delete
     * @return ResponseEntity with HTTP 204 NO CONTENT status
     * @throws EntityNotFoundException if bike path not found
     * @throws AccessDeniedException if user is not the creator
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBikePath(@PathVariable Long id) {
        bikePathService.deleteBikePath(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}