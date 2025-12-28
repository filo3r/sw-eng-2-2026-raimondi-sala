package it.polimi.se.bbp.service;

import it.polimi.se.bbp.dto.request.BikePathFinderRequest;
import it.polimi.se.bbp.entity.BikePath;
import it.polimi.se.bbp.entity.BikePathPoint;
import it.polimi.se.bbp.entity.Obstacle;
import it.polimi.se.bbp.geo.BoundingBox;
import it.polimi.se.bbp.geo.Coordinate;
import it.polimi.se.bbp.geo.GeoUtils;
import it.polimi.se.bbp.dto.result.GeocodeResult;
import it.polimi.se.bbp.repository.BikePathPointRepository;
import it.polimi.se.bbp.repository.BikePathRepository;
import it.polimi.se.bbp.repository.ObstacleRepository;
import it.polimi.se.bbp.service.mapbox.MapboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for finding bike paths based on geographic search criteria.
 * Finds published bike paths within specified radius of origin and destination addresses.
 * Uses efficient multi-step filtering strategy for optimal performance.
 * Only searches published (public) bike paths.
 */
@Service
@RequiredArgsConstructor
public class BikePathFinderService {

    /**
     * Repository for bike path data access.
     */
    private final BikePathRepository bikePathRepository;

    /**
     * Repository for bike path point data access.
     */
    private final BikePathPointRepository bikePathPointRepository;

    /**
     * Repository for obstacle data access.
     */
    private final ObstacleRepository obstacleRepository;

    /**
     * Service for geocoding addresses to coordinates.
     */
    private final MapboxService mapboxService;

    /**
     * Maximum page size to prevent excessive memory usage.
     */
    private static final int MAX_PAGE_SIZE = 10;

    /**
     * Finds published bike paths within geographic radius of origin and destination.
     * Multi-step filtering strategy: (1) geocode addresses, (2) calculate bounding boxes,
     * (3) query database with bounding boxes, (4) filter with precise Haversine distance,
     * (5) sort by score DESC, (6) paginate, (7) batch load relationships, (8) restore sort order.
     * Only published bike paths included, sorted by score (highest first).
     * @param request search criteria with addresses and radii in kilometers
     * @param page page number (0-indexed)
     * @param size results per page (positive, max MAX_PAGE_SIZE)
     * @return paginated list of matching bike paths with all relationships loaded
     * @throws IllegalArgumentException if pagination parameters are invalid
     */
    @Transactional(readOnly = true)
    public Page<BikePath> findBikePaths(BikePathFinderRequest request, int page, int size) {
        validatePaginationParameters(page, size);
        // STEP 1: Geocode addresses to coordinates
        GeoSearchCoordinates searchCoords = geocodeSearchAddresses(request);
        // STEP 2: Calculate bounding boxes for fast filtering
        BoundingBoxes boundingBoxes = calculateBoundingBoxes(searchCoords, request);
        // STEP 3: Query database with bounding boxes (indexed)
        List<BikePath> candidates = queryBikePathsWithinBoundingBoxes(boundingBoxes);
        // STEP 4: Filter with precise Haversine distance
        List<BikePath> matches = filterByPreciseDistance(candidates, searchCoords, request);
        // STEP 5: Sort by score descending
        List<BikePath> sortedMatches = sortByScoreDescending(matches);
        // STEP 6: Paginate sorted results
        PaginationResult paginationResult = paginateResults(sortedMatches, page, size);
        if (paginationResult.isEmpty())
            return Page.empty(PageRequest.of(page, size));
        // STEP 7: Batch load complete bike paths with relationships
        List<BikePath> completeBikePaths = loadCompleteBikePaths(paginationResult.pageIds());
        // STEP 8: Restore original sort order (batch loading returns arbitrary order)
        List<BikePath> sortedPage = sortByIdOrder(completeBikePaths, paginationResult.pageIds());
        return new PageImpl<>(
                sortedPage,
                PageRequest.of(page, size),
                paginationResult.totalElements()
        );
    }

    /**
     * Geocodes origin and destination addresses from search request.
     * Converts addresses to coordinates using Mapbox Geocoding API.
     * @param request search request with addresses to geocode
     * @return GeoSearchCoordinates with origin and destination coordinates
     */
    private GeoSearchCoordinates geocodeSearchAddresses(BikePathFinderRequest request) {
        GeocodeResult originGeocode = mapboxService.geocodeAddress(request.originAddress());
        GeocodeResult destGeocode = mapboxService.geocodeAddress(request.destinationAddress());
        return new GeoSearchCoordinates(
                originGeocode.coordinate(),
                destGeocode.coordinate()
        );
    }

    /**
     * Calculates bounding boxes around origin and destination coordinates.
     * Bounding boxes approximate circular search radius for fast database filtering.
     * @param searchCoords origin and destination coordinates
     * @param request search request with radius values
     * @return BoundingBoxes with origin and destination bounding boxes
     */
    private BoundingBoxes calculateBoundingBoxes(GeoSearchCoordinates searchCoords, BikePathFinderRequest request) {
        BoundingBox originBox = GeoUtils.calculateBoundingBox(
                searchCoords.origin(),
                request.originRadiusKm()
        );
        BoundingBox destBox = GeoUtils.calculateBoundingBox(
                searchCoords.destination(),
                request.destinationRadiusKm()
        );
        return new BoundingBoxes(originBox, destBox);
    }

    /**
     * Queries database for published bike paths within bounding boxes.
     * Fast operation using spatial indexes on lat/lon columns.
     * May include false positives from rectangular approximation.
     * @param boundingBoxes origin and destination bounding boxes
     * @return list of candidate bike paths
     */
    private List<BikePath> queryBikePathsWithinBoundingBoxes(BoundingBoxes boundingBoxes) {
        BoundingBox originBox = boundingBoxes.origin();
        BoundingBox destBox = boundingBoxes.destination();
        return bikePathRepository.findPublishedWithinBoundingBoxes(
                originBox.minLat(), originBox.maxLat(),
                originBox.minLon(), originBox.maxLon(),
                destBox.minLat(), destBox.maxLat(),
                destBox.minLon(), destBox.maxLon()
        );
    }

    /**
     * Filters candidates using precise Haversine distance calculation.
     * Eliminates false positives from rectangular bounding box approximation.
     * Only includes paths where both origin and destination are within specified radii.
     * @param candidates candidate bike paths from bounding box query
     * @param searchCoords search origin and destination coordinates
     * @param request search request with radius values
     * @return list of bike paths matching distance criteria
     */
    private List<BikePath> filterByPreciseDistance(
            List<BikePath> candidates,
            GeoSearchCoordinates searchCoords,
            BikePathFinderRequest request
    ) {
        return candidates.stream()
                .filter(bikePath -> {
                    // Convert entity lat/lon to Coordinate objects
                    Coordinate bikePathOrigin = Coordinate.toCoordinate(
                            bikePath.getOriginLatitude(),
                            bikePath.getOriginLongitude()
                    );
                    Coordinate bikePathDest = Coordinate.toCoordinate(
                            bikePath.getDestinationLatitude(),
                            bikePath.getDestinationLongitude()
                    );
                    // Calculate distances using Haversine
                    double originDistance = GeoUtils.haversineDistance(searchCoords.origin(), bikePathOrigin);
                    double destDistance = GeoUtils.haversineDistance(searchCoords.destination(), bikePathDest);
                    // Both must be within radius
                    return originDistance <= request.originRadiusKm()
                            && destDistance <= request.destinationRadiusKm();
                })
                .collect(Collectors.toList());
    }

    /**
     * Sorts bike paths by score descending (highest score first).
     * Higher scores indicate better quality and safety.
     * @param bikePaths list of bike paths to sort
     * @return new list sorted by score DESC
     */
    private List<BikePath> sortByScoreDescending(List<BikePath> bikePaths) {
        return bikePaths.stream()
                .sorted(Comparator.comparing(BikePath::getScore).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Applies pagination to sorted list of bike paths.
     * Extracts IDs for requested page.
     * @param sortedBikePaths sorted list of all matching bike paths
     * @param page page number (0-indexed)
     * @param size page size
     * @return PaginationResult with page IDs and total count
     */
    private PaginationResult paginateResults(List<BikePath> sortedBikePaths, int page, int size) {
        int totalElements = sortedBikePaths.size();
        int start = page * size;
        int end = Math.min(start + size, totalElements);
        if (start >= totalElements || sortedBikePaths.isEmpty())
            return new PaginationResult(List.of(), totalElements);
        // Extract IDs for this page
        List<Long> pageIds = sortedBikePaths.subList(start, end).stream()
                .map(BikePath::getId)
                .collect(Collectors.toList());
        return new PaginationResult(pageIds, totalElements);
    }

    /**
     * Loads complete bike paths with all relationships using batch loading.
     * Avoids N+1 problem with separate batch queries for points and obstacles.
     * @param bikePathIds list of bike path IDs to load
     * @return list of complete bike paths with relationships loaded
     */
    private List<BikePath> loadCompleteBikePaths(List<Long> bikePathIds) {
        // Fetch bike path entities without relationships
        List<BikePath> bikePaths = bikePathRepository.findAllById(bikePathIds);
        // Batch load and attach points and obstacles
        if (!bikePaths.isEmpty())
            enrichBikePathsWithPointsAndObstacles(bikePaths, bikePathIds);
        return bikePaths;
    }

    /**
     * Enriches bike paths with points and obstacles using batch loading.
     * Uses "batch load + group + assign" pattern to avoid N+1 problem.
     * Reduces 2N queries to 2 queries total (1 for all points + 1 for all obstacles).
     * Example: 20 bike paths = 40 queries reduced to 2 queries (20x improvement).
     * @param bikePaths list of bike paths to enrich
     * @param bikePathIds IDs of bike paths for batch loading
     */
    private void enrichBikePathsWithPointsAndObstacles(List<BikePath> bikePaths, List<Long> bikePathIds) {
        // Batch fetch all points in single query
        List<BikePathPoint> allPoints = bikePathPointRepository
                .findAllByBikePathIdInOrderByBikePathIdAscSequentialPositionAsc(bikePathIds);
        // Group points by bike path ID
        Map<Long, List<BikePathPoint>> pointsByBikePathId = allPoints.stream()
                .collect(Collectors.groupingBy(
                        point -> point.getBikePath().getId(),
                        Collectors.toList()
                ));
        // Batch fetch all obstacles in single query
        List<Obstacle> allObstacles = obstacleRepository
                .findAllByBikePathIdInOrderByBikePathIdAscPositionOnPathAsc(bikePathIds);
        // Group obstacles by bike path ID
        Map<Long, List<Obstacle>> obstaclesByBikePathId = allObstacles.stream()
                .collect(Collectors.groupingBy(
                        obstacle -> obstacle.getBikePath().getId(),
                        Collectors.toList()
                ));
        // Attach grouped collections to bike paths
        bikePaths.forEach(bikePath -> {
            List<BikePathPoint> points = pointsByBikePathId.getOrDefault(bikePath.getId(), new ArrayList<>());
            bikePath.setBikePathPoints(points);
            List<Obstacle> obstacles = obstaclesByBikePathId.getOrDefault(bikePath.getId(), new ArrayList<>());
            bikePath.setObstacles(obstacles);
        });
    }

    /**
     * Sorts bike paths according to specified order of IDs.
     * Preserves original sort order (by score DESC) after batch loading.
     * @param bikePaths list of bike paths to sort
     * @param orderedIds list of IDs in desired order
     * @return new list sorted according to orderedIds
     */
    private List<BikePath> sortByIdOrder(List<BikePath> bikePaths, List<Long> orderedIds) {
        // Create map for O(1) ID lookup
        Map<Long, BikePath> bikePathMap = bikePaths.stream()
                .collect(Collectors.toMap(BikePath::getId, bp -> bp));
        // Rebuild list in desired order
        return orderedIds.stream()
                .map(bikePathMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Validates pagination parameters are within acceptable ranges.
     * @param page page number (must be non-negative)
     * @param size page size (must be positive and not exceed MAX_PAGE_SIZE)
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
     * Record holding geocoded search coordinates.
     * @param origin origin coordinate
     * @param destination destination coordinate
     */
    private record GeoSearchCoordinates(Coordinate origin, Coordinate destination) {}

    /**
     * Record holding bounding boxes for origin and destination.
     * Used for fast database filtering with spatial indexes.
     * @param origin bounding box around origin
     * @param destination bounding box around destination
     */
    private record BoundingBoxes(BoundingBox origin, BoundingBox destination) {}

    /**
     * Record holding pagination results.
     * @param pageIds list of bike path IDs in this page
     * @param totalElements total number of matching bike paths across all pages
     */
    private record PaginationResult(List<Long> pageIds, int totalElements) {
        /**
         * Checks if pagination result is empty.
         * @return true if page is empty, false otherwise
         */
        public boolean isEmpty() {
            return pageIds.isEmpty();
        }
    }

}