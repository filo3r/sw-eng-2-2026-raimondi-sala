package it.polimi.se.bbp.service;

import it.polimi.se.bbp.dto.mapbox.GeocodeResult;
import it.polimi.se.bbp.dto.request.BikePathSearchRequest;
import it.polimi.se.bbp.entity.BikePath;
import it.polimi.se.bbp.repository.BikePathRepository;
import it.polimi.se.bbp.service.mapbox.MapboxService;
import it.polimi.se.bbp.utility.GeoUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Service for geographic search of bike paths.
 * Finds published bike paths within specified radius of origin and destination.
 * Uses 3-step filtering strategy: bounding box (fast DB filter), Haversine (precise), and pagination.
 */
@Service
@RequiredArgsConstructor
public class BikePathSearchService {

    /**
     * Repository for bike path data access operations.
     */
    private final BikePathRepository bikePathRepository;

    /**
     * Service for geocoding addresses to coordinates.
     */
    private final MapboxService mapboxService;

    /**
     * Maximum page size to prevent excessive memory usage.
     */
    private static final int MAX_PAGE_SIZE = 100;

    /**
     * Searches for published bike paths within geographic radius of origin and destination.
     * Strategy:
     * 1. Geocode addresses to coordinates
     * 2. Calculate bounding boxes for fast DB filtering
     * 3. Query database with bounding boxes (indexed, fast)
     * 4. Filter candidates with precise Haversine distance
     * 5. Sort by score DESC
     * 6. Paginate results
     * 7. Load complete bike paths with points and obstacles (3-step loading)
     * Only published bike paths are included in results.
     * @param request search criteria with addresses and radii
     * @param page page number (0-indexed)
     * @param size number of results per page
     * @return paginated list of matching bike paths with all relationships loaded
     * @throws IllegalArgumentException if addresses cannot be geocoded or parameters are invalid
     * @throws IllegalStateException if Mapbox service is unavailable
     */
    public Page<BikePath> searchBikePaths(BikePathSearchRequest request, int page, int size) {
        // Validate pagination parameters
        validatePaginationParameters(page, size);
        // STEP 1: Geocode addresses
        GeocodeResult originGeocode = mapboxService.geocodeAddress(request.getOriginAddress());
        GeocodeResult destGeocode = mapboxService.geocodeAddress(request.getDestinationAddress());
        double originLat = originGeocode.getCoordinate().getLatitude();
        double originLon = originGeocode.getCoordinate().getLongitude();
        double destLat = destGeocode.getCoordinate().getLatitude();
        double destLon = destGeocode.getCoordinate().getLongitude();
        // STEP 2: Calculate bounding boxes for fast DB filtering
        GeoUtils.BoundingBox originBox = GeoUtils.calculateBoundingBox(originLat, originLon, request.getOriginRadiusKm());
        GeoUtils.BoundingBox destBox = GeoUtils.calculateBoundingBox(destLat, destLon, request.getDestinationRadiusKm());
        // STEP 3: Query database with bounding boxes (fast, uses indexes)
        List<BikePath> candidates = bikePathRepository.findPublishedWithinBoundingBoxes(
                originBox.getMinLat(), originBox.getMaxLat(),
                originBox.getMinLon(), originBox.getMaxLon(),
                destBox.getMinLat(), destBox.getMaxLat(),
                destBox.getMinLon(), destBox.getMaxLon()
        );
        // STEP 4: Precise filtering with Haversine distance
        List<BikePath> matches = candidates.stream()
                .filter(bp -> {
                    // Calculate exact distance for origin
                    double originDistance = GeoUtils.haversineDistance(
                            originLat, originLon,
                            bp.getOriginLatitude(), bp.getOriginLongitude()
                    );
                    // Calculate exact distance for destination
                    double destDistance = GeoUtils.haversineDistance(
                            destLat, destLon,
                            bp.getDestinationLatitude(), bp.getDestinationLongitude()
                    );
                    // Both must be within radius
                    return originDistance <= request.getOriginRadiusKm()
                            && destDistance <= request.getDestinationRadiusKm();
                })
                .sorted(Comparator.comparing(BikePath::getScore).reversed()) // Sort by score DESC
                .collect(Collectors.toList());
        // STEP 5: Manual pagination
        int totalElements = matches.size();
        int start = page * size;
        int end = Math.min(start + size, totalElements);
        // Handle empty results or out of bounds
        if (start >= totalElements || matches.isEmpty()) {
            return Page.empty(PageRequest.of(page, size));
        }
        // Extract page of IDs
        List<Long> pageIds = matches.subList(start, end).stream()
                .map(BikePath::getId)
                .collect(Collectors.toList());
        // STEP 6: Load complete bike paths with relationships (3-step strategy)
        // Load bike paths with points
        List<BikePath> pageBikePaths = bikePathRepository.findByIdsWithPoints(pageIds);
        // Load obstacles (Hibernate merges into existing entities)
        if (!pageBikePaths.isEmpty()) {
            bikePathRepository.findByIdsWithObstacles(pageIds);
        }
        // STEP 7: Reorder according to score sorting (findByIdsWithPoints may return arbitrary order)
        pageBikePaths = sortBikePaths(pageBikePaths, pageIds);
        // Create Page with manual pagination metadata
        return new PageImpl<>(
                pageBikePaths,
                PageRequest.of(page, size),
                totalElements
        );
    }

    /**
     * Validates pagination parameters.
     * @param page page number (must be non-negative)
     * @param size page size (must be positive and not exceed maximum)
     * @throws IllegalArgumentException if parameters are invalid
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
     * Sorts bike paths according to a specified order of IDs.
     * Needed because findByIdsWithPoints may return results in arbitrary order.
     * @param bikePaths list of bike paths to sort
     * @param orderedIds list of IDs in desired order
     * @return list of bike paths sorted according to orderedIds
     */
    private List<BikePath> sortBikePaths(List<BikePath> bikePaths, List<Long> orderedIds) {
        // Create map for O(1) lookup by ID
        Map<Long, BikePath> bikePathMap = bikePaths.stream()
                .collect(Collectors.toMap(BikePath::getId, bp -> bp));
        // Build result list in correct order
        return orderedIds.stream()
                .map(bikePathMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

}