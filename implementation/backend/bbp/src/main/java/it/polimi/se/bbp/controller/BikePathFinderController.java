package it.polimi.se.bbp.controller;

import it.polimi.se.bbp.dto.request.BikePathFinderRequest;
import it.polimi.se.bbp.dto.response.PagedBikePathResponse;
import it.polimi.se.bbp.entity.BikePath;
import it.polimi.se.bbp.mapper.response.PagedBikePathResponseMapper;
import it.polimi.se.bbp.service.BikePathFinderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for bike path finder operations.
 * Handles geographic search to find published bike paths within specified radius.
 * This endpoint is public and searches across all published bike paths in the system.
 */
@RestController
@RequestMapping("/api/finder/bike-paths")
@RequiredArgsConstructor
public class BikePathFinderController {

    /**
     * Service for bike path finder operations.
     */
    private final BikePathFinderService bikePathFinderService;

    /**
     * Mapper for converting paginated bike paths to response DTOs.
     */
    private final PagedBikePathResponseMapper pagedBikePathResponseMapper;

    /**
     * Finds published bike paths within geographic radius of origin and destination.
     * Searches for paths where both origin and destination are within specified distances
     * from the search addresses. Only published bike paths are returned, private paths are excluded.
     * Results are ordered by score (highest first) and include complete route points and obstacles.
     * Search strategy: geocodes addresses, uses bounding boxes with spatial indexes for fast filtering,
     * applies Haversine distance calculation, and loads complete bike path data with batch loading.
     * @param request search criteria containing addresses and search radii in kilometers
     * @param page page number, 0-indexed (default: 0)
     * @param size number of results per page (default: 20, max: 100)
     * @return paginated search results with navigation metadata
     */
    @PostMapping
    public ResponseEntity<PagedBikePathResponse> findBikePaths(
            @Valid @RequestBody BikePathFinderRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<BikePath> bikePathPage = bikePathFinderService.findBikePaths(request, page, size);
        PagedBikePathResponse response = pagedBikePathResponseMapper.toPagedResponse(bikePathPage);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

}