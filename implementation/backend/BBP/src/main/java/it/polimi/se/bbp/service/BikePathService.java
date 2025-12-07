package it.polimi.se.bbp.service;

import it.polimi.se.bbp.dto.request.BikePathSearchRequest;
import it.polimi.se.bbp.entity.*;
import it.polimi.se.bbp.dto.result.CyclingRouteResult;
import it.polimi.se.bbp.dto.result.GeocodeResult;
import it.polimi.se.bbp.dto.request.BikePathManualCreateRequest;
import it.polimi.se.bbp.dto.request.BikePathUpdateRequest;
import it.polimi.se.bbp.mapper.entity.BikePathMapper;
import it.polimi.se.bbp.mapper.entity.BikePathPointMapper;
import it.polimi.se.bbp.repository.BikePathPointRepository;
import it.polimi.se.bbp.repository.BikePathRepository;
import it.polimi.se.bbp.repository.ObstacleRepository;
import it.polimi.se.bbp.geo.SpatialService;
import it.polimi.se.bbp.service.mapbox.MapboxService;
import it.polimi.se.bbp.geo.Coordinate;
import it.polimi.se.bbp.specification.BikePathSpecification;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.locationtech.jts.geom.Geometry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service for bike path operations.
 * Manages creation, updates, deletion, and retrieval using Mapbox for geocoding/routing
 * and SpatialService for geometry operations.
 * Delegates all obstacle persistence to ObstacleService.
 */
@Service
@RequiredArgsConstructor
public class BikePathService {

    /**
     * Repository for bike path data access.
     */
    private final BikePathRepository bikePathRepository;

    /**
     * Repository for bike path point data access.
     */
    private final BikePathPointRepository bikePathPointRepository;

    /**
     * Repository for obstacle data access (READ ONLY).
     * All write operations delegated to ObstacleService.
     */
    private final ObstacleRepository obstacleRepository;

    /**
     * Service for user authentication and retrieval.
     */
    private final UserAuthService userAuthService;

    /**
     * Service for Mapbox API interactions (geocoding and routing).
     */
    private final MapboxService mapboxService;

    /**
     * Service for obstacle operations and persistence.
     */
    private final ObstacleService obstacleService;

    /**
     * Service for advanced spatial operations using JTS.
     */
    private final SpatialService spatialService;

    /**
     * Mapper for converting request data to BikePath entities.
     */
    private final BikePathMapper bikePathMapper;

    /**
     * Mapper for converting route coordinates to BikePathPoint entities.
     */
    private final BikePathPointMapper bikePathPointMapper;

    /**
     * Buffer distance around route in meters.
     * Obstacles must be within this distance to be valid.
     */
    private static final double BUFFER_DISTANCE_METERS = 15.0;

    /**
     * Weight of status component in score calculation.
     */
    private static final double WEIGHT_STATUS = 0.5;

    /**
     * Weight of obstacles component in score calculation.
     */
    private static final double WEIGHT_OBSTACLES = 0.5;

    /**
     * Maximum page size to prevent excessive memory usage.
     */
    private static final int MAX_PAGE_SIZE = 100;

    /**
     * Creates new bike path with route and optional obstacles using batch insert.
     * Workflow: (1) authenticate user, (2) geocode addresses, (3) calculate cycling route,
     * (4) create bike path entity, (5) batch save path/points/obstacles (ObstacleService
     * handles complete obstacle lifecycle), (6) calculate and update score.
     * @param request bike path creation request with addresses, status, and obstacles
     * @return created bike path entity with all relationships loaded
     * @throws IllegalArgumentException if addresses invalid, route cannot be calculated, or obstacles too far
     */
    @Transactional
    public BikePath createBikePathManually(BikePathManualCreateRequest request) {
        User user = userAuthService.getAuthenticatedUser();
        OffsetDateTime now = OffsetDateTime.now();
        // Geocode addresses to coordinates
        List<GeocodeResult> geocodeResults = mapboxService.geocodeAddresses(request.addresses());
        // Calculate cycling route through waypoints
        CyclingRouteResult routeResult = calculateCyclingRoute(geocodeResults);
        // Create BikePath entity with calculated metrics
        BikePath bikePath = createBikePathEntity(request, user, now, geocodeResults, routeResult);
        // Save bike path and batch insert points/obstacles
        saveBikePathData(request, user, now, bikePath, routeResult);
        // Save to persist final score
        return bikePathRepository.save(bikePath);
    }

    /**
     * Updates existing bike path with complex permission rules.
     * Recalculates route buffer ONLY if new obstacles are added.
     * Permission rules: creator can always update; other users only if published.
     * Workflow: (1) validate permissions, (2) update fields, (3) calculate buffer if needed,
     * (4) update/add obstacles (delegates to ObstacleService), (5) update audit fields,
     * (6) recalculate score, (7) save.
     * @param bikePathId ID of bike path to update
     * @param request update request with fields to modify
     * @return updated bike path entity
     * @throws EntityNotFoundException if bike path not found
     * @throws AccessDeniedException if user lacks permission
     * @throws IllegalArgumentException if obstacle data invalid
     * @throws OptimisticLockException if version mismatch
     */
    @Transactional
    public BikePath updateBikePath(Long bikePathId, BikePathUpdateRequest request) {
        User user = userAuthService.getAuthenticatedUser();
        // Load bike path with all relationships
        BikePath bikePath = loadCompleteBikePath(bikePathId);
        // Validate update permissions
        validateUpdatePermissions(bikePath, user.getId());
        // Check optimistic locking
        checkOptimisticLock(request, bikePath);
        OffsetDateTime now = OffsetDateTime.now();
        // Update obstacles if requested (delegates to ObstacleService)
        updateBikePathObstacles(request, bikePath, user, now);
        // Update basic fields and recalculate score
        updateBikePathFields(request, bikePath, user, now);
        return bikePathRepository.save(bikePath);
    }

    /**
     * Deletes bike path by ID.
     * Only creator can delete. Associated data deleted via cascade.
     * @param bikePathId ID of bike path to delete
     * @throws EntityNotFoundException if bike path not found
     * @throws AccessDeniedException if user is not creator
     */
    @Transactional
    public void deleteBikePath(Long bikePathId) {
        User user = userAuthService.getAuthenticatedUser();
        BikePath bikePath = bikePathRepository.findById(bikePathId)
                .orElseThrow(() -> new EntityNotFoundException("Bike path not found"));
        // Only creator can delete
        if (!bikePath.getCreatedBy().getId().equals(user.getId()))
            throw new AccessDeniedException("You can only delete bike paths you created");
        bikePathRepository.delete(bikePath);
    }

    /**
     * Retrieves paginated list of bike paths created by authenticated user.
     * Returns both published and private bike paths with all relationships loaded.
     * Default sorting: createdAt DESC (newest first).
     * @param page page number (0-indexed)
     * @param size bike paths per page (positive, max MAX_PAGE_SIZE)
     * @param sortBy field name to sort by (default: createdAt)
     * @param direction sort direction ASC or DESC (default: DESC)
     * @return page of bike paths with relationships loaded
     * @throws IllegalArgumentException if pagination parameters invalid
     */
    @Transactional(readOnly = true)
    public Page<BikePath> getUserBikePaths(int page, int size, String sortBy, String direction) {
        User user = userAuthService.getAuthenticatedUser();
        // Fetch and enrich using template method
        return fetchAndEnrichBikePaths(page, size, sortBy, direction,
                pageable -> bikePathRepository.findPageByCreatedById(user.getId(), pageable));
    }

    /**
     * Searches bike paths for authenticated user based on filters.
     * All filters optional and combinable. Returns paginated list with all relationships loaded.
     * Default sorting: createdAt DESC (newest first).
     * Available filters: origin/destination (text search), createdAtFrom/createdAtTo (date range).
     * @param searchRequest search request with filter criteria
     * @param page page number (0-indexed)
     * @param size bike paths per page (positive, max MAX_PAGE_SIZE)
     * @param sortBy field name to sort by (default: createdAt)
     * @param direction sort direction ASC or DESC (default: DESC)
     * @return page of bike paths matching criteria with relationships loaded
     * @throws IllegalArgumentException if pagination parameters invalid
     */
    @Transactional(readOnly = true)
    public Page<BikePath> searchBikePaths(BikePathSearchRequest searchRequest, int page, int size, String sortBy, String direction) {
        User user = userAuthService.getAuthenticatedUser();
        BikePathSpecification specification = new BikePathSpecification(user.getId(), searchRequest);
        // Fetch and enrich using template method
        return fetchAndEnrichBikePaths(page, size, sortBy, direction,
                pageable -> bikePathRepository.findAll(specification, pageable));
    }

    /**
     * Template method for fetching and enriching bike paths with common pagination workflow.
     * Eliminates code duplication by extracting common setup, enrichment, and return logic.
     * Actual query delegated to provided function for flexibility.
     * @param page page number (0-indexed)
     * @param size page size
     * @param sortBy field to sort by
     * @param direction sort direction (ASC or DESC)
     * @param queryFunction function executing actual query given Pageable
     * @return page of bike paths with relationships loaded and enriched
     */
    private Page<BikePath> fetchAndEnrichBikePaths(int page, int size, String sortBy, String direction,
                                                   Function<Pageable, Page<BikePath>> queryFunction) {
        // Validate and create pageable
        validatePaginationParameters(page, size);
        Pageable pageable = createPageable(page, size, sortBy, direction);
        // Execute query (delegated)
        Page<BikePath> bikePathPage = queryFunction.apply(pageable);
        // Batch load points and obstacles
        if (!bikePathPage.isEmpty())
            enrichBikePathsWithPointsAndObstacles(bikePathPage);
        return bikePathPage;
    }

    /**
     * Calculates cycling route between multiple waypoints.
     * Extracts coordinates from geocode results and calls Mapbox routing.
     * @param geocodeResults list of geocoded addresses with coordinates
     * @return calculated cycling route with distance and coordinates
     */
    private CyclingRouteResult calculateCyclingRoute(List<GeocodeResult> geocodeResults) {
        List<Coordinate> waypoints = GeocodeResult.extractCoordinates(geocodeResults);
        return mapboxService.calculateCyclingRoute(waypoints);
    }

    /**
     * Creates BikePath entity with all calculated metrics.
     * Initializes with zero score (calculated after obstacles created).
     * @param request bike path creation request
     * @param user authenticated user creating bike path
     * @param now current timestamp
     * @param geocodeResults geocoded addresses
     * @param routeResult calculated cycling route
     * @return BikePath entity ready to save
     */
    private BikePath createBikePathEntity(BikePathManualCreateRequest request, User user, OffsetDateTime now, List<GeocodeResult> geocodeResults, CyclingRouteResult routeResult) {
        GeocodeResult origin = geocodeResults.getFirst();
        GeocodeResult destination = geocodeResults.getLast();
        BigDecimal totalDistanceKm = calculateDistance(routeResult);
        return bikePathMapper.toEntity(request, user, now, null, null, origin, destination, BigDecimal.ZERO, totalDistanceKm);
    }

    /**
     * Calculates total distance in kilometers from route result.
     * Converts meters to kilometers with 3 decimal places.
     * @param routeResult calculated cycling route
     * @return total distance in kilometers
     */
    private BigDecimal calculateDistance(CyclingRouteResult routeResult) {
        return BigDecimal.valueOf(routeResult.distanceInMeters() / 1000.0).setScale(3, RoundingMode.HALF_UP);
    }

    /**
     * Saves bike path and associated data with batch insert optimization.
     * Creates route buffer once and reuses for all obstacle validations.
     * Maintains relationships in memory to avoid database reload.
     * Delegates obstacle persistence to ObstacleService (creation, relationships, positions).
     * @param request bike path creation request
     * @param user authenticated user
     * @param now current timestamp
     * @param bikePath bike path entity to save
     * @param routeResult calculated cycling route
     */
    private void saveBikePathData(BikePathManualCreateRequest request, User user, OffsetDateTime now, BikePath bikePath, CyclingRouteResult routeResult) {
        // Save bike path to get generated ID
        bikePathRepository.save(bikePath);
        // Create and batch insert bike path points
        List<BikePathPoint> bikePathPoints = bikePathPointMapper.toEntities(routeResult.routeCoordinates(), bikePath, null);
        bikePathPointRepository.saveAll(bikePathPoints);
        bikePath.setBikePathPoints(bikePathPoints);
        // Create route buffer for obstacle validation
        List<Coordinate> routeCoordinates = routeResult.routeCoordinates();
        Geometry routeBuffer = spatialService.createRouteBuffer(routeCoordinates, BUFFER_DISTANCE_METERS);
        // Delegate obstacle management to ObstacleService
        obstacleService.createAndSaveObstacles(
                request.obstacles(),
                bikePath,
                routeCoordinates,
                routeBuffer,
                user,
                now
        );
        // Calculate final score
        List<Obstacle> obstacles = new ArrayList<>(bikePath.getObstacles());
        bikePath.setScore(calculateScore(bikePath, obstacles));
    }

    /**
     * Calculates overall score based on status and obstacles.
     * Pure function that doesn't modify entity.
     * Special handling: if statusScore null (UNDER_MAINTENANCE, TEMPORARILY_CLOSED,
     * PERMANENTLY_CLOSED), returns 0.0 regardless of obstacles.
     * For operational statuses: weighted average of status score (normalized to [0,5])
     * and obstacle score (logistic model, [0,5]).
     * Final score range [0.0, 5.0] with 2 decimal precision.
     * @param bikePath bike path entity (reads status and distance only)
     * @param obstacles list of obstacles associated with bike path
     * @return calculated score in range [0.0, 5.0]
     */
    private BigDecimal calculateScore(BikePath bikePath, List<Obstacle> obstacles) {
        Integer statusScore = bikePath.getStatus().getStatusScore();
        // Non-operational status = 0.0 score
        if (statusScore == null)
            return BigDecimal.ZERO;
        // Calculate status component (normalized to [0, 5])
        double scoreStatus = statusScore / 2.0;
        // Calculate obstacle component using logistic model
        BigDecimal scoreObstacles = obstacleService.calculateObstacleScore(obstacles, bikePath.getTotalDistance());
        // Weighted average
        double scoreFinal = (WEIGHT_STATUS * scoreStatus) + (WEIGHT_OBSTACLES * scoreObstacles.doubleValue());
        return BigDecimal.valueOf(scoreFinal).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Validates permissions for updating bike path.
     * Creator always allowed; other users only if published.
     * @param bikePath bike path being updated
     * @param userId ID of user attempting update
     * @throws AccessDeniedException if user lacks permission
     */
    private void validateUpdatePermissions(BikePath bikePath, Long userId) {
        boolean isCreator = bikePath.getCreatedBy().getId().equals(userId);
        boolean isPublished = bikePath.getPublished();
        if (!isCreator && !isPublished)
            throw new AccessDeniedException("You can only update your own private bike paths");
    }

    /**
     * Checks if bike path version matches request version for optimistic locking.
     * @param request update request
     * @param bikePath bike path entity
     * @throws OptimisticLockException if versions don't match
     */
    private void checkOptimisticLock(BikePathUpdateRequest request, BikePath bikePath) {
        if (!bikePath.getVersion().equals(request.version()))
            throw new OptimisticLockException("Bike path has been modified by another user. Please refresh and try again");
    }

    /**
     * Updates obstacles for bike path if requested.
     * Recalculates route buffer only if new obstacles added.
     * Delegates all obstacle logic to ObstacleService (updates, creation, persistence,
     * relationships, position recalculation).
     * @param request update request
     * @param bikePath bike path entity
     * @param user authenticated user
     * @param now current timestamp
     */
    private void updateBikePathObstacles(BikePathUpdateRequest request, BikePath bikePath, User user, OffsetDateTime now) {
        if (hasObstacleUpdates(request)) {
            // Prepare route data (coordinates and optional buffer)
            RouteData routeData = prepareRouteDataForObstacleUpdate(bikePath, request);
            // Delegate to ObstacleService
            obstacleService.updateAndSaveObstacles(
                    bikePath,
                    request.obstaclesToAdd(),
                    request.obstaclesToUpdate(),
                    routeData.coordinates(),
                    routeData.buffer(),
                    user,
                    now
            );
        }
    }

    /**
     * Prepares route data (coordinates and optional buffer) for obstacle updates.
     * Buffer calculated only if adding new obstacles (performance optimization).
     * @param bikePath bike path entity
     * @param request update request
     * @return RouteData with coordinates and optional buffer geometry
     */
    private RouteData prepareRouteDataForObstacleUpdate(BikePath bikePath, BikePathUpdateRequest request) {
        // Extract route coordinates
        List<Coordinate> routeCoordinates = extractCoordinatesFromPoints(
                new ArrayList<>(bikePath.getBikePathPoints())
        );
        // Calculate buffer only if adding new obstacles
        Geometry routeBuffer = null;
        boolean needsBuffer = request.obstaclesToAdd() != null && !request.obstaclesToAdd().isEmpty();
        if (needsBuffer)
            routeBuffer = spatialService.createRouteBuffer(routeCoordinates, BUFFER_DISTANCE_METERS);
        return new RouteData(routeCoordinates, routeBuffer);
    }

    /**
     * Checks if update request contains obstacle modifications.
     * @param request update request
     * @return true if obstacles to add or update, false otherwise
     */
    private boolean hasObstacleUpdates(BikePathUpdateRequest request) {
        boolean hasAdditions = request.obstaclesToAdd() != null && !request.obstaclesToAdd().isEmpty();
        boolean hasUpdates = request.obstaclesToUpdate() != null && !request.obstaclesToUpdate().isEmpty();
        return hasAdditions || hasUpdates;
    }

    /**
     * Extracts coordinates from bike path points.
     * @param points list of bike path points
     * @return list of coordinates in sequential order
     */
    private List<Coordinate> extractCoordinatesFromPoints(List<BikePathPoint> points) {
        return points.stream()
                .map(p -> Coordinate.toCoordinate(p.getLatitude(), p.getLongitude()))
                .collect(Collectors.toList());
    }

    /**
     * Updates basic fields of bike path and recalculates score.
     * @param request update request
     * @param bikePath bike path entity
     * @param user authenticated user
     * @param now current timestamp
     */
    private void updateBikePathFields(BikePathUpdateRequest request, BikePath bikePath, User user, OffsetDateTime now) {
        if (request.status() != null)
            bikePath.setStatus(request.status());
        if (request.description() != null)
            bikePath.setDescription(request.description());
        if (request.published() != null)
            bikePath.setPublished(request.published());
        // Update audit fields
        bikePath.setUpdatedBy(user);
        bikePath.setUpdatedAt(now);
        // Recalculate score
        List<Obstacle> obstacles = new ArrayList<>(bikePath.getObstacles());
        BigDecimal score = calculateScore(bikePath, obstacles);
        bikePath.setScore(score);
    }

    /**
     * Loads complete bike path with all relationships from database.
     * Fetches bike path and manually loads points and obstacles in sequential order.
     * @param bikePathId ID of bike path to load
     * @return complete bike path entity with relationships loaded
     * @throws EntityNotFoundException if bike path not found
     */
    private BikePath loadCompleteBikePath(Long bikePathId) {
        BikePath bikePath = bikePathRepository.findById(bikePathId)
                .orElseThrow(() -> new EntityNotFoundException("Bike path not found"));
        // Eagerly load relationships
        Hibernate.initialize(bikePath.getBikePathPoints());
        Hibernate.initialize(bikePath.getObstacles());
        return bikePath;
    }

    /**
     * Validates pagination parameters are within acceptable ranges.
     * @param page page number (must be non-negative)
     * @param size page size (must be positive and not exceed MAX_PAGE_SIZE)
     * @throws IllegalArgumentException if parameters invalid
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
     * Creates Pageable object with pagination and sorting parameters.
     * Defaults to DESC if direction not ASC.
     * @param page page number
     * @param size page size
     * @param sortBy field to sort by
     * @param direction sort direction (ASC or DESC)
     * @return configured Pageable object
     */
    private Pageable createPageable(int page, int size, String sortBy, String direction) {
        Sort.Direction sortDirection = "ASC".equalsIgnoreCase(direction)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
    }

    /**
     * Enriches page of bike paths by batch loading points and obstacles.
     * Fetches all points and obstacles in two separate queries to avoid N+1 problem.
     * Groups by bike path ID and assigns to corresponding bike paths.
     * @param bikePathPage page of bike paths to enrich
     */
    private void enrichBikePathsWithPointsAndObstacles(Page<BikePath> bikePathPage) {
        // Extract bike path IDs
        List<Long> bikePathIds = bikePathPage.getContent().stream()
                .map(BikePath::getId)
                .toList();
        // Batch fetch all points
        List<BikePathPoint> allPoints = bikePathPointRepository
                .findAllByBikePathIdInOrderByBikePathIdAscSequentialPositionAsc(bikePathIds);
        // Group points by bike path ID
        Map<Long, List<BikePathPoint>> pointsByBikePathId = allPoints.stream()
                .collect(Collectors.groupingBy(
                        point -> point.getBikePath().getId(),
                        Collectors.toList()
                ));
        // Batch fetch all obstacles (READ only)
        List<Obstacle> allObstacles = obstacleRepository
                .findAllByBikePathIdInOrderByBikePathIdAscPositionOnPathAsc(bikePathIds);
        // Group obstacles by bike path ID
        Map<Long, List<Obstacle>> obstaclesByBikePathId = allObstacles.stream()
                .collect(Collectors.groupingBy(
                        obstacle -> obstacle.getBikePath().getId(),
                        Collectors.toList()
                ));
        // Assign to bike paths
        bikePathPage.forEach(bikePath -> {
            List<BikePathPoint> points = pointsByBikePathId.getOrDefault(bikePath.getId(), new ArrayList<>());
            bikePath.setBikePathPoints(points);
            List<Obstacle> obstacles = obstaclesByBikePathId.getOrDefault(bikePath.getId(), new ArrayList<>());
            bikePath.setObstacles(obstacles);
        });
    }

    /**
     * Record holding route data for obstacle operations.
     * @param coordinates route coordinates
     * @param buffer optional buffer geometry (null if not calculated)
     */
    private record RouteData(List<Coordinate> coordinates, Geometry buffer) {}

}