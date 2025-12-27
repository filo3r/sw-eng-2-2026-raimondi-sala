package it.polimi.se.bbp.controller;

import it.polimi.se.bbp.dto.request.BikePathManualCreateRequest;
import it.polimi.se.bbp.dto.request.BikePathSearchRequest;
import it.polimi.se.bbp.dto.request.BikePathUpdateRequest;
import it.polimi.se.bbp.dto.response.BikePathResponse;
import it.polimi.se.bbp.dto.response.PagedBikePathResponse;
import it.polimi.se.bbp.entity.BikePath;
import it.polimi.se.bbp.mapper.response.BikePathResponseMapper;
import it.polimi.se.bbp.mapper.response.PagedBikePathResponseMapper;
import it.polimi.se.bbp.service.BikePathService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for bike path management.
 * Handles creation, updates, deletion, and retrieval of bike paths.
 * All endpoints require JWT authentication.
 */
@RestController
@RequestMapping("/api/bike-paths")
@RequiredArgsConstructor
public class BikePathController {

    /**
     * Service for bike path business logic.
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
     * Retrieves bike path by ID with all relationships.
     * Public bike paths accessible to all users.
     * Private bike paths only accessible to creator.
     * @param id bike path ID
     * @return bike path with points and obstacles
     */
    @GetMapping("/{id}")
    public ResponseEntity<BikePathResponse> getBikePathById(@PathVariable Long id) {
        BikePath bikePath = bikePathService.getBikePathById(id);
        BikePathResponse response = bikePathResponseMapper.toResponse(bikePath);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * Retrieves paginated bike paths created by the authenticated user.
     * Returns both published and private paths with all associated data including points and obstacles.
     * Uses 3-step loading strategy to eagerly load relationships while maintaining pagination efficiency.
     * @param page page number, 0-indexed (default: 0)
     * @param size number of items per page (default: 20, max: 100)
     * @param sortBy field to sort by (default: createdAt, options: score, totalDistance, origin, destination)
     * @param direction sort direction ASC or DESC (default: DESC)
     * @return paginated bike path response with navigation metadata
     */
    @GetMapping
    public ResponseEntity<PagedBikePathResponse> getUserBikePaths(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction
    ) {
        Page<BikePath> bikePathPage = bikePathService.getUserBikePaths(page, size, sortBy, direction);
        PagedBikePathResponse response = pagedBikePathResponseMapper.toPagedResponse(bikePathPage);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * Searches bike paths with filters and pagination.
     * Applies search criteria from request body and returns matching results.
     * @param searchRequest filter criteria for bike path search
     * @param page page number, 0-indexed (default: 0)
     * @param size number of items per page (default: 20, max: 100)
     * @param sortBy field to sort by (default: createdAt)
     * @param direction sort direction ASC or DESC (default: DESC)
     * @return paginated search results
     */
    @PostMapping("/search")
    public ResponseEntity<PagedBikePathResponse> searchBikePaths(
            @Valid @RequestBody BikePathSearchRequest searchRequest,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction
    ) {
        Page<BikePath> bikePathPage = bikePathService.searchBikePaths(searchRequest, page, size, sortBy, direction);
        PagedBikePathResponse response = pagedBikePathResponseMapper.toPagedResponse(bikePathPage);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * Creates a new bike path from manual user input.
     * Geocodes addresses, calculates cycling route, validates obstacles proximity,
     * and stores bike path with GPS coordinates and obstacles.
     * Published paths are visible to all users and editable by anyone.
     * Private paths are only visible and editable by the creator.
     * Obstacles must be within 15 meters of the route to be accepted.
     * Workflow: parallel geocoding, route calculation, geometric buffer creation,
     * batch insert of points and obstacles, score calculation.
     * @param request bike path creation data including addresses, status, obstacles, and metadata
     * @return created bike path with all relationships
     */
    @PostMapping("/manual")
    public ResponseEntity<BikePathResponse> createBikePathManually(@Valid @RequestBody BikePathManualCreateRequest request) {
        BikePath bikePath = bikePathService.createBikePathManually(request);
        BikePathResponse response = bikePathResponseMapper.toResponse(bikePath);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates an existing bike path with partial field updates.
     * Supports updates to status, published flag, and obstacles.
     * Permission rules: creator can always update, other users can only update published paths.
     * New obstacles are geocoded and validated against route buffer (15 meters).
     * Score is automatically recalculated after updates.
     * Uses optimistic locking (version field) to prevent concurrent modification conflicts.
     * @param id bike path ID to update
     * @param request update data including fields to modify and current version for optimistic locking
     * @return updated bike path with recalculated score
     */
    @PatchMapping("/{id}")
    public ResponseEntity<BikePathResponse> updateBikePath(@PathVariable Long id, @Valid @RequestBody BikePathUpdateRequest request) {
        BikePath bikePath = bikePathService.updateBikePath(id, request);
        BikePathResponse response = bikePathResponseMapper.toResponse(bikePath);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * Deletes a bike path by ID.
     * Only the creator can delete their bike path.
     * All associated data (points, obstacles) are cascade deleted.
     * This operation is irreversible.
     * @param id bike path ID to delete
     * @return empty response with 204 status
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBikePath(@PathVariable Long id) {
        bikePathService.deleteBikePath(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}