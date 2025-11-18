package it.polimi.se.bbp.service;

import it.polimi.se.bbp.dto.mapbox.GeocodeResult;
import it.polimi.se.bbp.dto.request.ObstacleCreateRequest;
import it.polimi.se.bbp.dto.request.ObstacleUpdateRequest;
import it.polimi.se.bbp.entity.BikePath;
import it.polimi.se.bbp.entity.Obstacle;
import it.polimi.se.bbp.entity.User;
import it.polimi.se.bbp.mapper.entity.ObstacleMapper;
import it.polimi.se.bbp.service.mapbox.MapboxService;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for handling obstacle operations.
 * Manages obstacle creation, updates, and validation against bike path routes.
 * Uses JTS Topology Suite for efficient geometric validation.
 */
@Service
@RequiredArgsConstructor
public class ObstacleService {

    /**
     * Service for interacting with Mapbox APIs (geocoding).
     */
    private final MapboxService mapboxService;

    /**
     * Mapper for converting obstacle request data to Obstacle entities.
     */
    private final ObstacleMapper obstacleMapper;

    /**
     * GeometryFactory for creating JTS geometric objects.
     * Thread-safe and reusable.
     */
    private final GeometryFactory geometryFactory;

    /**
     * Parameter k for the logistic model used in obstacle score calculation.
     * Controls the sensitivity of the penalty: lower k = more severe, higher k = more tolerant.
     * Interpretation guide:
     * - k = 1.0: Severe
     * - k = 1.5: Moderate
     * - k = 2.0: Tolerant
     * - k = 3.0: Very tolerant
     */
    private static final double K = 2.0;

    /**
     * OPTIMIZED: Creates obstacles validating them against the bike path route buffer.
     * The buffer is calculated ONCE by the caller and reused for all obstacles.
     * This method performs batch geocoding and batch validation for maximum performance.
     * Workflow:
     * 1. Geocode all obstacle addresses in parallel (performance)
     * 2. Validate all obstacles against the route buffer in a single pass (batch)
     * 3. Create Obstacle entities
     * @param obstacleRequests list of requests to create obstacles
     * @param bikePath the bike path entity to which obstacles belong
     * @param routeBuffer the geometric buffer of the route (already calculated by caller)
     * @param createdBy user creating the obstacles
     * @param createdAt timestamp of creation
     * @return list of created and validated obstacles
     * @throws IllegalArgumentException if an obstacle address is invalid or too far from route
     * @throws IllegalStateException if Mapbox geocoding service is unavailable
     */
    public List<Obstacle> createObstacles(List<ObstacleCreateRequest> obstacleRequests, BikePath bikePath, Geometry routeBuffer, User createdBy, OffsetDateTime createdAt) {
        // Handle empty obstacle list
        if (obstacleRequests == null || obstacleRequests.isEmpty()) {
            return new ArrayList<>();
        }
        // Step 1: Geocode ALL addresses in parallel (performance optimization)
        List<String> addresses = obstacleRequests.stream()
                .map(ObstacleCreateRequest::getAddress)
                .collect(Collectors.toList());
        List<GeocodeResult> geocodeResults = mapboxService.geocodeAddressesParallel(addresses);
        // Step 2: Validate ALL obstacles against the buffer (batch validation)
        validateObstaclesProximity(geocodeResults, routeBuffer);
        // Step 3: Create Obstacle entities
        List<Obstacle> obstacles = new ArrayList<>();
        for (int i = 0; i < obstacleRequests.size(); i++) {
            Obstacle obstacle = obstacleMapper.toEntity(
                    obstacleRequests.get(i),
                    bikePath,
                    createdBy,
                    createdAt,
                    null,
                    null,
                    geocodeResults.get(i),
                    true
            );
            obstacles.add(obstacle);
        }
        return obstacles;
    }

    /**
     * OPTIMIZED: Updates existing obstacles and adds new ones to a bike path.
     * The route buffer is calculated ONCE by the caller and reused for new obstacles only.
     * Workflow:
     * 1. Update existing obstacles (partial update - only non-null fields)
     * 2. Create and add new obstacles (using the pre-calculated buffer)
     * @param bikePath the bike path to update
     * @param obstaclesToAdd new obstacles to add (nullable)
     * @param obstaclesToUpdate existing obstacles to modify (nullable)
     * @param routeBuffer buffer of the route (pre-calculated, used only for new obstacles)
     * @param updatedBy user performing the update
     * @param updatedAt timestamp of update
     * @throws IllegalArgumentException if obstacle ID not found or new obstacle too far from route
     * @throws IllegalStateException if Mapbox geocoding service is unavailable
     */
    public void updateObstacles(BikePath bikePath, List<ObstacleCreateRequest> obstaclesToAdd, List<ObstacleUpdateRequest> obstaclesToUpdate, Geometry routeBuffer, User updatedBy, OffsetDateTime updatedAt) {
        // Update existing obstacles (no geocoding needed)
        if (obstaclesToUpdate != null && !obstaclesToUpdate.isEmpty()) {
            updateExistingObstacles(bikePath, obstaclesToUpdate, updatedBy, updatedAt);
        }
        // Add new obstacles (requires geocoding and validation)
        if (obstaclesToAdd != null && !obstaclesToAdd.isEmpty()) {
            List<Obstacle> newObstacles = createObstacles(obstaclesToAdd, bikePath, routeBuffer, updatedBy, updatedAt);
            bikePath.getObstacles().addAll(newObstacles);
        }
    }

    /**
     * Updates fields of existing obstacles (partial update).
     * Only non-null fields in the update request are modified.
     * Validates that all obstacles to update actually belong to the bike path.
     * @param bikePath the bike path containing the obstacles
     * @param updates list of obstacle update requests
     * @param updatedBy user performing the update
     * @param updatedAt timestamp of update
     * @throws IllegalArgumentException if an obstacle ID is not found in the bike path
     */
    private void updateExistingObstacles(BikePath bikePath, List<ObstacleUpdateRequest> updates, User updatedBy, OffsetDateTime updatedAt) {
        // Create a map for efficient obstacle lookup by ID
        Map<Long, Obstacle> obstacleMap = bikePath.getObstacles().stream()
                .collect(Collectors.toMap(Obstacle::getId, obstacle -> obstacle));
        // Process each update request
        for (ObstacleUpdateRequest update : updates) {
            Obstacle obstacle = obstacleMap.get(update.getId());
            // Validate that the obstacle belongs to this bike path
            if (obstacle == null)
                throw new IllegalArgumentException("Obstacle with ID " + update.getId() + " not found in this bike path");
            //
            if (!obstacle.getVersion().equals(update.getVersion()))
                throw new OptimisticLockException("Obstacle with ID " + update.getId() + " has been modified by another user. Please refresh and try again");
            // Partial update - only update non-null fields
            if (update.getType() != null)
                obstacle.setType(update.getType());
            if (update.getSeverity() != null)
                obstacle.setSeverity(update.getSeverity());
            if (update.getActive() != null)
                obstacle.setActive(update.getActive());
            // Update audit fields
            obstacle.setUpdatedBy(updatedBy);
            obstacle.setUpdatedAt(updatedAt);
        }
    }

    /**
     * Validates that ALL obstacles are within the route buffer using JTS geometric operations.
     * Uses batch validation for optimal performance - the buffer is passed in pre-calculated.
     * This method ensures that reported obstacles are actually on or near the bike path,
     * preventing users from adding irrelevant obstacles far from the route.
     * @param geocodeResults geocoded coordinates of the obstacles
     * @param routeBuffer geometric buffer around the bike path route
     * @throws IllegalArgumentException if any obstacle is outside the buffer (too far from route)
     */
    private void validateObstaclesProximity(List<GeocodeResult> geocodeResults, Geometry routeBuffer) {
        for (GeocodeResult result : geocodeResults) {
            // Create JTS Point for the obstacle
            Point obstaclePoint = geometryFactory.createPoint(new Coordinate(result.getCoordinate().getLongitude(), result.getCoordinate().getLatitude()));
            // Check if the point is within the buffer
            if (!routeBuffer.contains(obstaclePoint))
                throw new IllegalArgumentException("Obstacle at address '" + result.getAddress() + "' is too far from the bike path route");
        }
    }

    /**
     * Calculates the obstacle component of the bike path score using an inverted logistic model.
     * Algorithm:
     * 1. Filter only active obstacles (active=true)
     * 2. If no active obstacles: return 5.0 (perfect score)
     * 3. Calculate average severity across active obstacles
     * 4. Calculate impact density: (numActive × avgSeverity) / distance
     * 5. Apply logistic model: scoreObstacles = 5.0 / (1 + impact/k)
     * The logistic model ensures:
     * - No obstacles → score = 5.0 (maximum)
     * - Low impact → score ≈ 4-5 (minor penalty)
     * - Medium impact → score ≈ 2-3 (significant penalty)
     * - High impact → score → 0 (severe penalty, asymptotic)
     * The parameter k controls sensitivity:
     * - Lower k = more severe penalties
     * - Higher k = more tolerant system
     * @param obstacles list of obstacles associated with the bike path
     * @param totalDistance total distance of the bike path in km
     * @return obstacle score in range [0, 5] with 2 decimal precision
     */
    public BigDecimal calculateObstacleScore(List<Obstacle> obstacles, BigDecimal totalDistance) {
        // Filter active obstacles only
        List<Obstacle> activeObstacles = obstacles.stream()
                .filter(Obstacle::getActive)
                .toList();
        // If no active obstacles, perfect obstacle score
        if (activeObstacles.isEmpty())
            return BigDecimal.valueOf(5.0).setScale(2, RoundingMode.HALF_UP);
        // Calculate average severity level
        double avgSeverity = activeObstacles.stream()
                .mapToInt(obstacle -> obstacle.getSeverity().getSeverityLevel())
                .average()
                .orElse(0.0);
        // Get distance in km (ensure minimum to avoid division by zero)
        double distance = totalDistance.doubleValue();
        distance = Math.max(distance, 0.001); // Minimum 1 meter
        // Calculate impact density: (number × severity) / distance
        int numActive = activeObstacles.size();
        double impact = (numActive * avgSeverity) / distance;
        // Apply inverted logistic model: 5.0 / (1 + impact/k)
        double obstacleScore = 5.0 / (1.0 + (impact / K));
        // Return as BigDecimal with 2 decimal precision
        return BigDecimal.valueOf(obstacleScore).setScale(2, RoundingMode.HALF_UP);
    }

}