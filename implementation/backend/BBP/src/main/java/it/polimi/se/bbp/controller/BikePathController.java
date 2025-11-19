package it.polimi.se.bbp.controller;

import it.polimi.se.bbp.dto.request.BikePathManualCreateRequest;
import it.polimi.se.bbp.dto.request.BikePathUpdateRequest;
import it.polimi.se.bbp.dto.response.BikePathResponse;
import it.polimi.se.bbp.dto.response.PagedBikePathResponse;
import it.polimi.se.bbp.entity.BikePath;
import it.polimi.se.bbp.mapper.response.BikePathResponseMapper;
import it.polimi.se.bbp.mapper.response.PagedBikePathResponseMapper;
import it.polimi.se.bbp.service.BikePathService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

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
     * Mapper for converting paginated BikePath results to PagedBikePathResponse DTOs.
     */
    private final PagedBikePathResponseMapper pagedBikePathResponseMapper;

    /**
     * Retrieves a paginated list of bike paths created by the authenticated user.
     * Returns both published and private bike paths with all associated data including bike path points and obstacles.
     * Supports pagination and sorting to efficiently handle large datasets.
     * Uses 3-step loading strategy to eagerly load all relationships while maintaining pagination.
     * Query parameters:
     * - page: Page number (0-indexed, default: 0)
     * - size: Number of bike paths per page (default: 20, max: 100)
     * - sortBy: Field to sort by (default: createdAt, examples: score, totalDistance, origin, destination)
     * - direction: Sort direction (default: DESC, values: ASC or DESC)
     * Example requests:
     * - GET /api/bike-paths (default: page 0, size 20, sort by createdAt DESC)
     * - GET /api/bike-paths?page=1&size=10 (second page with 10 items)
     * - GET /api/bike-paths?page=0&size=50&sortBy=score&direction=DESC (first page, 50 items, sorted by score descending)
     * - GET /api/bike-paths?sortBy=totalDistance&direction=ASC (sorted by distance ascending)
     * Response includes:
     * - content: List of bike paths with all details (points and obstacles)
     * - currentPage: Current page number
     * - pageSize: Number of items per page
     * - totalElements: Total number of bike paths
     * - totalPages: Total number of pages
     * - hasNext: Whether there is a next page
     * - hasPrevious: Whether there is a previous page
     * - firstPage: Whether this is the first page
     * - lastPage: Whether this is the last page
     * @param page the page number (0-indexed, default: 0)
     * @param size the number of bike paths per page (default: 20, max: 100)
     * @param sortBy the field to sort by (default: createdAt)
     * @param direction the sort direction: ASC or DESC (default: DESC)
     * @return ResponseEntity with HTTP 200 OK status and paginated bike path response
     * @throws IllegalArgumentException if pagination parameters are invalid
     */
    @GetMapping
    public ResponseEntity<PagedBikePathResponse> getUserBikePaths(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction
    ) {
        // Get paginated bike paths from service (3-step loading)
        Page<BikePath> bikePathPage = bikePathService.getUserBikePaths(page, size, sortBy, direction);
        // Map to PagedBikePathResponse DTO
        PagedBikePathResponse response = pagedBikePathResponseMapper.toPagedResponse(bikePathPage);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * Creates a new bike path from user input.
     * Geocodes addresses, calculates cycling route, validates obstacles proximity,
     * and stores bike path with GPS coordinates and obstacles.
     * The bike path is created with the specified status and published flag.
     * If published is true, the bike path will be visible to all users and can be updated by anyone.
     * If published is false, the bike path is private and only visible/editable by the creator.
     * Obstacles are validated to ensure they are within the acceptable distance from the route (15 meters).
     * The bike path score is automatically calculated based on status and obstacles.
     * Workflow:
     * 1. Geocode all addresses in parallel using Mapbox API
     * 2. Calculate optimal cycling route through all waypoints
     * 3. Create geometric buffer around route for obstacle validation
     * 4. Batch insert bike path points for performance
     * 5. Validate and batch insert obstacles
     * 6. Calculate final score based on status and obstacles
     * 7. Return complete bike path with all relationships
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
     * Updates an existing bike path with complex permission rules.
     * Supports partial updates - only provided fields will be modified.
     * Permission rules:
     * - The creator can always update their bike path (regardless of published status)
     * - Other users can only update published bike paths
     * - Private bike paths can only be updated by their creator
     * Updatable fields:
     * - status: Change the condition/maintenance status (affects score calculation)
     * - published: Change visibility (public/private)
     * - obstaclesToAdd: Add new obstacles to the bike path (with geocoding and validation)
     * - obstaclesToUpdate: Modify existing obstacles (type, severity, active status)
     * New obstacles are geocoded, validated against the route buffer (15 meters), and batch inserted.
     * The bike path score is automatically recalculated after updates.
     * Uses optimistic locking (version field) to prevent concurrent modification conflicts.
     * Route buffer is recalculated only if new obstacles are being added (performance optimization).
     * @param id the ID of the bike path to update
     * @param request the update request containing fields to modify and current version for optimistic locking
     * @return ResponseEntity with HTTP 200 OK status and updated bike path response
     * @throws EntityNotFoundException if bike path not found
     * @throws AccessDeniedException if user lacks permission to update
     * @throws OptimisticLockException if bike path was modified by another user
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
     * This operation is irreversible.
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