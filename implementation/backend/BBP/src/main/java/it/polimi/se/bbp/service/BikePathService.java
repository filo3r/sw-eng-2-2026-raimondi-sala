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
import it.polimi.se.bbp.repository.BikePathRepository;
import it.polimi.se.bbp.repository.UserRepository;
import it.polimi.se.bbp.service.mapbox.MapboxService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
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
     * Creates a new bike path with route and optional obstacles.
     * OPTIMIZED: Calculates route buffer ONCE and reuses it for all obstacle validations.
     * Workflow:
     * 1. Geocode bike path addresses in parallel
     * 2. Calculate cycling route through all waypoints
     * 3. Create route buffer using JTS (ONCE)
     * 4. Create bike path entity with points
     * 5. Create and validate obstacles using the pre-calculated buffer
     * 6. Calculate final score based on status and obstacles
     * 7. Save bike path (cascade saves points, obstacles, and score)
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
        // Step 5: Create BikePath entity (score will be calculated later)
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
        // Step 6: Create BikePathPoint entities from route coordinates
        List<BikePathPoint> points = bikePathPointMapper.toEntities(
                routeResult.getRouteCoordinates(),
                bikePath,
                null
        );
        bikePath.setBikePathPoints(points);
        // Step 7: Create and validate Obstacles (reuses the pre-calculated buffer)
        List<Obstacle> obstacles = obstacleService.createObstacles(
                request.getObstacles(),
                bikePath,
                routeBuffer,
                user,
                now
        );
        bikePath.setObstacles(obstacles);
        // Step 8: Calculate final score based on status and obstacles
        calculateScore(bikePath);
        // Step 9: Save bike path (cascade saves points, obstacles, and calculated score)
        return bikePathRepository.save(bikePath);
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
        BikePath bikePath = bikePathRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Bike path not found"));
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
        return bikePathRepository.save(bikePath);
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
     * Retrieves all bike paths created by the authenticated user.
     * Returns both published and private bike paths.
     * @return list of bike paths belonging to the user
     */
    public List<BikePath> getUserBikePaths() {
        Long userId = getCurrentUserId();
        return bikePathRepository.findAllByCreatedById(userId);
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