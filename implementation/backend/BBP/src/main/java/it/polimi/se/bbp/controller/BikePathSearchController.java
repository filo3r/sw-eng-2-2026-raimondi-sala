package it.polimi.se.bbp.controller;

import it.polimi.se.bbp.dto.request.BikePathSearchRequest;
import it.polimi.se.bbp.dto.response.PagedBikePathResponse;
import it.polimi.se.bbp.entity.BikePath;
import it.polimi.se.bbp.mapper.response.PagedBikePathResponseMapper;
import it.polimi.se.bbp.service.BikePathSearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for bike path search operations.
 * Handles geographic search of published bike paths within specified radius.
 */
@RestController
@RequestMapping("/api/search/bike-paths")
@RequiredArgsConstructor
public class BikePathSearchController {

    /**
     * Service for bike path geographic search operations.
     */
    private final BikePathSearchService bikePathSearchService;

    /**
     * Mapper for converting paginated bike paths to response DTOs.
     */
    private final PagedBikePathResponseMapper pagedBikePathResponseMapper;

    /**
     * Searches for published bike paths within geographic radius of origin and destination.
     * Finds bike paths where both origin and destination are within specified distances
     * from the search addresses. Results are ordered by score (highest first).
     * Returns complete bike paths with all points and obstacles.
     * Uses efficient 3-step filtering: bounding boxes (fast DB filter), Haversine (precise),
     * and 3-step loading for relationships.
     * Example request:
     * POST /api/search/bike-paths?page=0&size=20
     * {
     *   "originAddress": "Piazza Duomo, Milano, Italy",
     *   "originRadiusKm": 0.5,
     *   "destinationAddress": "Stazione Centrale, Milano, Italy",
     *   "destinationRadiusKm": 1.0
     * }
     * @param request search criteria with addresses and radii
     * @param page page number (0-indexed, default: 0)
     * @param size number of results per page (default: 20, max: 100)
     * @return paginated search results with complete bike path data
     * @throws IllegalArgumentException if addresses cannot be geocoded or parameters are invalid
     * @throws IllegalStateException if Mapbox service is unavailable
     */
    @PostMapping
    public ResponseEntity<PagedBikePathResponse> searchBikePaths(
            @Valid @RequestBody BikePathSearchRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        // Execute search with pagination
        Page<BikePath> bikePathPage = bikePathSearchService.searchBikePaths(request, page, size);
        // Map to PagedBikePathResponse DTO (includes all points and obstacles)
        PagedBikePathResponse response = pagedBikePathResponseMapper.toPagedResponse(bikePathPage);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

}