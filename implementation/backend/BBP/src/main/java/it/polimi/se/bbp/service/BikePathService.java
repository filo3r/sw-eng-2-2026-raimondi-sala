package it.polimi.se.bbp.service;

import it.polimi.se.bbp.dto.mapbox.Coordinate;
import it.polimi.se.bbp.dto.mapbox.CyclingRouteResult;
import it.polimi.se.bbp.dto.mapbox.GeocodeResult;
import it.polimi.se.bbp.dto.request.BikePathManualCreateRequest;
import it.polimi.se.bbp.dto.request.BikePathUpdateRequest;
import it.polimi.se.bbp.entity.BikePath;
import it.polimi.se.bbp.entity.BikePathPoint;
import it.polimi.se.bbp.entity.Obstacle;
import it.polimi.se.bbp.entity.User;
import it.polimi.se.bbp.mapper.entity.BikePathMapper;
import it.polimi.se.bbp.mapper.entity.BikePathPointMapper;
import it.polimi.se.bbp.repository.BikePathPointRepository;
import it.polimi.se.bbp.repository.BikePathRepository;
import it.polimi.se.bbp.repository.ObstacleRepository;
import it.polimi.se.bbp.repository.UserRepository;
import it.polimi.se.bbp.service.mapbox.MapboxService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Service for handling bike path operations.
 * Manages bike path creation, updates, deletion, and retrieval using Mapbox APIs
 * for geocoding and routing, and JTS Topology Suite for obstacle validation.
 */
@Service
@RequiredArgsConstructor
public class BikePathService {

    /**
     * Repository for bike path data access operations.
     */
    private final BikePathRepository bikePathRepository;

    /**
     * Repository for user data access operations.
     */
    private final UserRepository userRepository;

    /**
     * Repository for bike path point data access operations.
     */
    private final BikePathPointRepository bikePathPointRepository;

    /**
     * Repository for obstacle data access operations.
     */
    private final ObstacleRepository obstacleRepository;

    /**
     * Service for interacting with Mapbox APIs (geocoding and routing).
     */
    private final MapboxService mapboxService;

    /**
     * Service for handling obstacle operations.
     */
    private final ObstacleService obstacleService;

    /**
     * Factory for creating JTS geometric objects.
     */
    private final GeometryFactory geometryFactory;

    /**
     * Mapper for converting bike path request data to BikePath entities.
     */
    private final BikePathMapper bikePathMapper;

    /**
     * Mapper for converting route coordinates to BikePathPoint entities.
     */
    private final BikePathPointMapper bikePathPointMapper;

    /**
     * Entity manager for JPA operations.
     */
    private final EntityManager entityManager;

    /**
     * Buffer distance around the bike path route in meters.
     * Obstacles must be within this distance to be considered valid.
     */
    private static final double BUFFER_DISTANCE_METERS = 15.0;

    /**
     * Weight of the status component in the final score calculation.
     */
    private static final double WEIGHT_STATUS = 0.5;

    /**
     * Weight of the obstacles component in the final score calculation.
     */
    private static final double WEIGHT_OBSTACLES = 0.5;

    /**
     * Creates a new bike path with route and optional obstacles using BATCH INSERT.
     * OPTIMIZED: Uses batch insert for BikePathPoints and Obstacles to improve performance.
     * Workflow:
     * 1. Geocode bike path addresses in parallel
     * 2. Calculate cycling route through all waypoints
     * 3. Create route buffer using JTS (ONCE)
     * 4. Save bike path entity first (without points and obstacles)
     * 5. BATCH INSERT bike path points
     * 6. Create, validate, and BATCH INSERT obstacles using the pre-calculated buffer
     * 7. Calculate final score based on status and obstacles
     * 8. Update bike path with calculated score
     * 9. Reload complete entity with all relationships
     * @param request the bike path creation request containing addresses, status, and obstacles
     * @return the created bike path entity with all associated data
     * @throws IllegalArgumentException if addresses are invalid, route cannot be calculated, or obstacles are too far
     * @throws IllegalStateException if Mapbox service is unavailable
     */
    @Transactional
    public BikePath createBikePathManual(BikePathManualCreateRequest request) {
        // Get current authenticated user
        Long userId = getCurrentUserId();
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalStateException("Authenticated user not found in database"));
        OffsetDateTime now = OffsetDateTime.now();
        // Step 1: Geocode bike path addresses in parallel (performance)
        List<GeocodeResult> geocodeResults = mapboxService.geocodeAddressesParallel(request.getAddresses());
        List<Coordinate> waypoints = extractCoordinates(geocodeResults);
        // Step 2: Calculate cycling route through all waypoints
        CyclingRouteResult routeResult = mapboxService.calculateCyclingRoute(waypoints);
        // Step 3: Create route buffer using JTS - ONCE for all obstacle validations
        Geometry routeBuffer = createRouteBuffer(routeResult.getRouteCoordinates());
        // Step 4: Extract origin, destination, and calculate total distance
        GeocodeResult origin = geocodeResults.getFirst();
        GeocodeResult destination = geocodeResults.getLast();
        BigDecimal totalDistanceKm = calculateDistance(routeResult);
        // Step 5: Create and save BikePath entity FIRST (with temporary score = 0)
        BikePath bikePath = bikePathMapper.toEntity(
                request,
                user,
                now,
                null,
                null,
                origin,
                destination,
                BigDecimal.ZERO,
                totalDistanceKm
        );
        bikePath = bikePathRepository.save(bikePath);
        // Step 6: BATCH INSERT - Create and save BikePathPoint entities
        List<BikePathPoint> points = bikePathPointMapper.toEntities(
                routeResult.getRouteCoordinates(),
                bikePath,
                null
        );
        bikePathPointRepository.saveAll(points);
        entityManager.flush();
        // Step 7: BATCH INSERT - Create, validate, and save Obstacles
        List<Obstacle> obstacles = obstacleService.createObstacles(
                request.getObstacles(),
                bikePath,
                routeBuffer,
                user,
                now
        );
        if (!obstacles.isEmpty())
            obstacleRepository.saveAll(obstacles);
        entityManager.flush();
        // Step 8: Calculate final score based on status and obstacles
        bikePath.setObstacles(obstacles);
        calculateScore(bikePath);
        // Step 9: Update bike path with calculated score
        bikePath = bikePathRepository.save(bikePath);
        entityManager.clear();
        // Step 10: Reload complete entity with all relationships (points + obstacles)
        bikePath = bikePathRepository.findByIdWithPoints(bikePath.getId()).orElseThrow(() -> new IllegalStateException("BikePath not found after save"));
        bikePathRepository.findByIdWithObstacles(bikePath.getId());
        return bikePath;
    }

    /**
     * Updates an existing bike path with complex permission rules.
     * OPTIMIZED: Recalculates route buffer ONLY if new obstacles are being added.
     * Permission rules:
     * - Creator can always update (published or private)
     * - Other users can only update published bike paths
     * - Only status, published flag, and obstacles can be updated
     * Workflow:
     * 1. Validate update permissions
     * 2. Update basic fields (status, published)
     * 3. Calculate buffer only if new obstacles need to be added
     * 4. Update/add obstacles
     * 5. Update audit fields
     * 6. Recalculate score based on updated status and/or obstacles
     * 7. Save bike path (cascade saves obstacle updates and new score)
     * @param id the ID of the bike path to update
     * @param request the update request containing fields to modify
     * @return the updated bike path entity
     * @throws EntityNotFoundException if bike path not found
     * @throws AccessDeniedException if user lacks permission to update
     * @throws IllegalArgumentException if obstacle data is invalid
     * @throws IllegalStateException if Mapbox service is unavailable (for new obstacles)
     */
    @Transactional
    public BikePath updateBikePath(Long id, BikePathUpdateRequest request) {
        // Get current authenticated user
        Long userId = getCurrentUserId();
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalStateException("Authenticated user not found in database"));
        // Load bike path from database
        BikePath bikePath = bikePathRepository.findByIdWithPoints(id).orElseThrow(() -> new EntityNotFoundException("Bike path not found"));
        bikePathRepository.findByIdWithObstacles(bikePath.getId());
        // Step 1: Validate complex update permissions
        validateUpdatePermissions(bikePath, userId);
        if (!bikePath.getVersion().equals(request.getVersion()))
            throw new OptimisticLockException("Bike path has been modified by another user. Please refresh and try again");
        OffsetDateTime now = OffsetDateTime.now();
        // Step 2: Update basic fields (partial update - only non-null fields)
        if (request.getStatus() != null)
            bikePath.setStatus(request.getStatus());
        if (request.getPublished() != null)
            bikePath.setPublished(request.getPublished());
        // Step 3: Handle obstacles (buffer calculated only if new obstacles are being added)
        boolean needsBufferRecalculation = request.getObstaclesToAdd() != null && !request.getObstaclesToAdd().isEmpty();
        if (needsBufferRecalculation || hasObstacleUpdates(request)) {
            Geometry routeBuffer = null;
            if (needsBufferRecalculation) {
                // Extract coordinates from existing bike path points
                List<Coordinate> routeCoordinates = extractCoordinatesFromPoints(bikePath.getBikePathPoints());
                routeBuffer = createRouteBuffer(routeCoordinates);
            }
            // Step 4: Update/add obstacles using ObstacleService
            obstacleService.updateObstacles(
                    bikePath,
                    request.getObstaclesToAdd(),
                    request.getObstaclesToUpdate(),
                    routeBuffer,
                    user,
                    now
            );
        }
        // Step 5: Update audit fields
        bikePath.setUpdatedBy(user);
        bikePath.setUpdatedAt(now);
        // Step 6: Recalculate score based on updated status and/or obstacles
        calculateScore(bikePath);
        // Step 7: Save bike path (JPA automatic flush with @Transactional)
        bikePath = bikePathRepository.save(bikePath);
        Hibernate.initialize(bikePath.getObstacles());
        return bikePath;
    }

    /**
     * Deletes a bike path by ID.
     * Only the creator can delete their bike path.
     * All associated data (points, obstacles) are deleted via cascade.
     * @param id the ID of the bike path to delete
     * @throws EntityNotFoundException if bike path not found
     * @throws AccessDeniedException if user is not the creator
     */
    @Transactional
    public void deleteBikePath(Long id) {
        Long userId = getCurrentUserId();
        BikePath bikePath = bikePathRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Bike path not found"));
        // Only the creator can delete
        if (!bikePath.getCreatedBy().getId().equals(userId))
            throw new AccessDeniedException("You can only delete bike paths you created");
        bikePathRepository.delete(bikePath);
    }

    /**
     * Retrieves a paginated list of bike paths created by the authenticated user.
     * OPTIMIZED: Uses 3-step loading strategy to avoid MultipleBagFetchException with pagination.
     * Returns both published and private bike paths with all relationships eagerly loaded.
     * Each bike path includes all bike path points and obstacles.
     * Supports sorting by any BikePath field (e.g., createdAt, score, totalDistance).
     * Default sorting: createdAt DESC (newest first).
     * 3-step loading strategy:
     * 1. Query page of bike path IDs with pagination and sorting
     * 2. Load bike paths with points for those specific IDs
     * 3. Load obstacles for those specific IDs (Hibernate merges into existing entities)
     * 4. Reorder bike paths according to original sort order from step 1
     * This approach is necessary because Spring Data cannot create a Page with multiple JOIN FETCH
     * (would throw MultipleBagFetchException). By loading IDs first, we maintain pagination
     * metadata while still eagerly loading all relationships.
     * @param page the page number (0-indexed, first page is 0)
     * @param size the number of bike paths per page (must be positive)
     * @param sortBy the field name to sort by (default: createdAt)
     * @param direction the sort direction: ASC or DESC (default: DESC)
     * @return page of bike paths with all relationships loaded and pagination metadata
     * @throws IllegalArgumentException if page or size parameters are invalid
     */
    public Page<BikePath> getUserBikePaths(int page, int size, String sortBy, String direction) {
        // Get authenticated user ID
        Long userId = getCurrentUserId();
        // Validate pagination parameters
        validatePaginationParameters(page, size);
        // Create Pageable with sorting
        Pageable pageable = createPageable(page, size, sortBy, direction);
        // STEP 1: Query page of bike path IDs only (with pagination and sorting)
        Page<Long> idPage = bikePathRepository.findIdsByCreatedById(userId, pageable);
        // Return empty page if no bike paths found
        if (idPage.isEmpty())
            return Page.empty(pageable);
        // Extract IDs from the page
        List<Long> ids = idPage.getContent();
        // STEP 2: Load bike paths with points for the IDs in this page
        List<BikePath> bikePaths = bikePathRepository.findByIdsWithPoints(ids);
        // STEP 3: Load obstacles for the same IDs (Hibernate merges into existing entities)
        bikePathRepository.findByIdsWithObstacles(ids);
        // STEP 4: Reorder bike paths to match the original sort order from step 1
        // This is necessary because findByIdsWithPoints may return results in arbitrary order
        bikePaths = sortBikePaths(bikePaths, ids);
        // Manually create Page object with the loaded bike paths and original pagination metadata
        // The Page interface expects: content, pageable, and total elements count
        return new org.springframework.data.domain.PageImpl<>(bikePaths, pageable, idPage.getTotalElements());
    }

    /**
     * Validates pagination parameters to ensure they are within acceptable ranges.
     * @param page the page number (must be non-negative)
     * @param size the page size (must be positive)
     * @throws IllegalArgumentException if parameters are invalid
     */
    private void validatePaginationParameters(int page, int size) {
        if (page < 0)
            throw new IllegalArgumentException("Page number must be non-negative");
        if (size <= 0)
            throw new IllegalArgumentException("Page size must be positive");
    }

    /**
     * Creates a Pageable object with the specified pagination and sorting parameters.
     * Converts sort direction string to Sort.Direction enum.
     * @param page the page number
     * @param size the page size
     * @param sortBy the field to sort by
     * @param direction the sort direction (ASC or DESC)
     * @return configured Pageable object
     */
    private Pageable createPageable(int page, int size, String sortBy, String direction) {
        // Parse sort direction (default to DESC if invalid)
        Sort.Direction sortDirection = "ASC".equalsIgnoreCase(direction)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        // Create and return Pageable
        return PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
    }

    /**
     * Sorts a list of bike paths according to a specified order of IDs.
     * This method is necessary because findByIdsWithPoints may return results in arbitrary order,
     * but we need to maintain the sort order specified in the original paginated query.
     * Creates a map for O(1) lookup, then builds the result list in the correct order.
     * @param bikePaths the list of bike paths to sort (may be in arbitrary order)
     * @param orderedIds the list of IDs in the desired order (from the paginated query)
     * @return list of bike paths sorted according to orderedIds
     */
    private List<BikePath> sortBikePaths(List<BikePath> bikePaths, List<Long> orderedIds) {
        // Create map for O(1) lookup by ID
        Map<Long, BikePath> bikePathMap = bikePaths.stream()
                .collect(Collectors.toMap(BikePath::getId, bp -> bp));
        // Build result list in the correct order
        return orderedIds.stream()
                .map(bikePathMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Creates a geometric buffer around the bike path route using JTS.
     * The buffer uses the default distance defined by BUFFER_DISTANCE_METERS (15 meters).
     * The buffer represents an acceptable distance for obstacles to be considered
     * part of this bike path.
     * The buffer accounts for latitude variation to provide accurate distance representation.
     * @param routeCoordinates coordinates forming the bike path route
     * @return JTS Geometry representing the buffered area around the route
     */
    private Geometry createRouteBuffer(List<Coordinate> routeCoordinates) {
        // Convert Coordinate DTOs to JTS Coordinate array (longitude, latitude)
        org.locationtech.jts.geom.Coordinate[] jtsCoords = routeCoordinates.stream()
                .map(c -> new org.locationtech.jts.geom.Coordinate(c.getLongitude(), c.getLatitude()))
                .toArray(org.locationtech.jts.geom.Coordinate[]::new);
        // Create LineString from coordinates
        LineString lineString = geometryFactory.createLineString(jtsCoords);
        // Calculate buffer distance in degrees accounting for geographic distortion
        double bufferDegrees = calculateBufferDegrees(routeCoordinates);
        // Create and return the buffer geometry
        return lineString.buffer(bufferDegrees);
    }

    /**
     * Calculates the buffer distance in decimal degrees accounting for latitude variation.
     * Geographic consideration:
     * - At the equator: 1 degree ≈ 111 km (both lat and lon)
     * - At higher latitudes: longitude degrees compress (111 km × cos(latitude))
     * - Latitude degrees remain constant (~111 km per degree globally)
     * This method calculates the average buffer in degrees by:
     * 1. Converting meters to latitude degrees (constant conversion)
     * 2. Converting meters to longitude degrees (latitude-dependent conversion)
     * 3. Averaging the two to approximate a circular buffer
     * @param routeCoordinates coordinates of the route to calculate average latitude
     * @return buffer distance in decimal degrees adjusted for geographic distortion
     */
    private double calculateBufferDegrees(List<Coordinate> routeCoordinates) {
        // Calculate buffer in latitude degrees (always ~111 km per degree)
        double bufferDegreesLat = BUFFER_DISTANCE_METERS / 111000.0;
        // Calculate average latitude of the route
        double averageLatitude = routeCoordinates.stream()
                .mapToDouble(Coordinate::getLatitude)
                .average()
                .orElse(0.0);
        // Convert average latitude to radians for cosine calculation
        double latRad = Math.toRadians(averageLatitude);
        // Calculate buffer in longitude degrees accounting for latitude compression
        double bufferDegreesLon = BUFFER_DISTANCE_METERS / (111000.0 * Math.cos(latRad));
        // Use average of latitude and longitude buffers to approximate a circular buffer in real meters
        // This approach accounts for geographic distortion and creates a more accurate validation area
        return (bufferDegreesLat + bufferDegreesLon) / 2.0;
    }

    /**
     * Validates permissions for updating a bike path.
     * Complex permission rules:
     * - Creator: always allowed to update (regardless of published status)
     * - Other users: only allowed if bike path is published
     * - Private bike paths: only creator can update
     * @param bikePath the bike path being updated
     * @param userId the ID of the user attempting the update
     * @throws AccessDeniedException if user lacks permission
     */
    private void validateUpdatePermissions(BikePath bikePath, Long userId) {
        boolean isCreator = bikePath.getCreatedBy().getId().equals(userId);
        boolean isPublished = bikePath.getPublished();
        // If not creator and path is not published, deny access
        if (!isCreator && !isPublished)
            throw new AccessDeniedException("You can only update your own private bike paths");
    }

    /**
     * Calculates the overall score of a bike path based on its status and obstacles.
     * Special handling for non-operational statuses:
     * - If statusScore is null (UNDER_MAINTENANCE, TEMPORARILY_CLOSED, PERMANENTLY_CLOSED),
     *   the bike path is considered unusable and the score is set to 0.0 regardless of obstacles.
     * For operational statuses, the score is a weighted average of two components:
     * 1. Status score: Derived from the bike path status (user evaluation)
     * 2. Obstacle score: Calculated from active obstacles, their severity, and distance
     * Formula (when statusScore is not null):
     * scoreStatus = statusScore / 2                    (normalized to [0, 5])
     * scoreObstacles = obstacleService.calculate(...)  (logistic model, range [0, 5])
     * scoreFinal = weightStatus × scoreStatus + weightObstacles × scoreObstacles
     * The weights are configurable (default 0.5/0.5 = equal importance).
     * The final score represents the overall quality and safety of the bike path:
     * - 0.0: Not operational (maintenance, closed) or extremely dangerous
     * - 0.0-1.5: Poor/dangerous, avoid
     * - 1.5-2.5: Below average, use with caution
     * - 2.5-3.5: Average/acceptable
     * - 3.5-4.5: Good/recommended
     * - 4.5-5.0: Excellent
     * @param bikePath the bike path entity to calculate the score for
     */
    private void calculateScore(BikePath bikePath) {
        // Get status score from bike path status
        Integer statusScore = bikePath.getStatus().getStatusScore();
        // If status score is null, the bike path is not operational (maintenance/closed)
        // Score is 0.0 regardless of obstacles
        if (statusScore == null) {
            bikePath.setScore(BigDecimal.ZERO);
            return;
        }
        // Calculate status component (normalized to [0, 5])
        double scoreStatus = statusScore / 2.0;
        // Calculate obstacle component using logistic model
        BigDecimal scoreObstacles = obstacleService.calculateObstacleScore(bikePath.getObstacles(), bikePath.getTotalDistance());
        // Weighted average of the two components
        double scoreFinal = (WEIGHT_STATUS * scoreStatus) + (WEIGHT_OBSTACLES * scoreObstacles.doubleValue());
        // Set the calculated score (2 decimal precision)
        bikePath.setScore(BigDecimal.valueOf(scoreFinal).setScale(2, RoundingMode.HALF_UP));
    }

    /**
     * Checks if the update request contains any obstacle modifications.
     * Used to determine if ObstacleService needs to be called for updates.
     * @param request the update request
     * @return true if there are obstacles to add or update, false otherwise
     */
    private boolean hasObstacleUpdates(BikePathUpdateRequest request) {
        boolean hasAdditions = request.getObstaclesToAdd() != null && !request.getObstaclesToAdd().isEmpty();
        boolean hasUpdates = request.getObstaclesToUpdate() != null && !request.getObstaclesToUpdate().isEmpty();
        return hasAdditions || hasUpdates;
    }

    /**
     * Extracts coordinates from geocode results to use as route waypoints.
     * @param geocodeResults list of geocode results
     * @return list of coordinates
     */
    private List<Coordinate> extractCoordinates(List<GeocodeResult> geocodeResults) {
        return geocodeResults.stream()
                .map(GeocodeResult::getCoordinate)
                .collect(Collectors.toList());
    }

    /**
     * Extracts coordinates from bike path points.
     * Used when updating a bike path to rebuild the route buffer.
     * @param points list of bike path points
     * @return list of coordinates in sequential order
     */
    private List<Coordinate> extractCoordinatesFromPoints(List<BikePathPoint> points) {
        return points.stream()
                .map(p -> Coordinate.builder()
                        .latitude(p.getLatitude())
                        .longitude(p.getLongitude())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Calculates the total distance in kilometers from the route result.
     * Converts meters to kilometers with 3 decimal places precision.
     * @param routeResult the calculated cycling route
     * @return total distance in kilometers
     */
    private BigDecimal calculateDistance(CyclingRouteResult routeResult) {
        return BigDecimal.valueOf(routeResult.getDistanceInMeters() / 1000.0)
                .setScale(3, RoundingMode.HALF_UP);
    }

    /**
     * Retrieves the authenticated user's ID from the security context.
     * The userId is stored as principal by JwtAuthFilter after validating the JWT token.
     * @return user ID of the authenticated user
     */
    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

}