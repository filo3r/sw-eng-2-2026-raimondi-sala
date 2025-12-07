package it.polimi.se.bbp.service;

import it.polimi.se.bbp.exception.obstacle.ObstacleNotFoundException;
import it.polimi.se.bbp.exception.obstacle.ObstacleTooFarException;
import it.polimi.se.bbp.geo.Coordinate;
import it.polimi.se.bbp.dto.result.GeocodeResult;
import it.polimi.se.bbp.dto.request.ObstacleCreateRequest;
import it.polimi.se.bbp.dto.request.ObstacleUpdateRequest;
import it.polimi.se.bbp.entity.BikePath;
import it.polimi.se.bbp.entity.Obstacle;
import it.polimi.se.bbp.entity.User;
import it.polimi.se.bbp.mapper.entity.ObstacleMapper;
import it.polimi.se.bbp.repository.ObstacleRepository;
import it.polimi.se.bbp.geo.SpatialService;
import it.polimi.se.bbp.service.mapbox.MapboxService;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Geometry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service for obstacle operations.
 * Manages creation, updates, validation against routes, score calculation, and persistence.
 * Has full responsibility for obstacle lifecycle: business logic, database persistence,
 * and bidirectional relationship management with BikePath.
 * Uses SpatialService for geometric validation.
 */
@Service
@RequiredArgsConstructor
public class ObstacleService {

    /**
     * Repository for obstacle data access.
     */
    private final ObstacleRepository obstacleRepository;

    /**
     * Service for Mapbox API interactions (geocoding).
     */
    private final MapboxService mapboxService;

    /**
     * Service for advanced spatial operations using JTS.
     */
    private final SpatialService spatialService;

    /**
     * Mapper for converting request data to Obstacle entities.
     */
    private final ObstacleMapper obstacleMapper;

    /**
     * Parameter k for logistic model in score calculation.
     * Controls penalty sensitivity: lower k = severe, higher k = tolerant.
     * Current value (2.0) = moderate tolerance.
     */
    private static final double K = 2.0;

    /**
     * Maximum score value (perfect score when no obstacles).
     */
    private static final double MAX_SCORE = 5.0;

    /**
     * Minimum distance in kilometers to prevent division by zero.
     * Equivalent to 1 meter.
     */
    private static final double MIN_DISTANCE_KM = 0.001;

    /**
     * Default active status for new obstacles.
     */
    private static final boolean DEFAULT_ACTIVE_STATUS = true;

    /**
     * Temporary position used before final calculation.
     * Position 0 replaced by actual position after recalculateAllObstaclePositions.
     */
    private static final int TEMPORARY_POSITION = 0;

    /**
     * Creates and saves obstacles validating them against route buffer.
     * Workflow: validate input → geocode → validate proximity → create entities →
     * batch save → add to bikePath → recalculate positions.
     * @param obstacleRequests list of obstacle creation requests
     * @param bikePath bike path entity (must be saved with ID)
     * @param routeCoordinates ordered route coordinates
     * @param routeBuffer geometric buffer around route (pre-calculated)
     * @param createdBy user creating obstacles
     * @param createdAt creation timestamp
     * @throws IllegalArgumentException if input invalid or obstacle too far from route
     */
    @Transactional
    public void createAndSaveObstacles(List<ObstacleCreateRequest> obstacleRequests, BikePath bikePath,
                                       List<Coordinate> routeCoordinates, Geometry routeBuffer,
                                       User createdBy, OffsetDateTime createdAt) {
        if (obstacleRequests == null || obstacleRequests.isEmpty())
            return;
        validateCreateObstaclesInput(bikePath, routeCoordinates, routeBuffer, createdBy, createdAt);
        processNewObstacles(obstacleRequests, bikePath, routeCoordinates, routeBuffer, createdBy, createdAt);
    }

    /**
     * Updates existing obstacles and creates new ones for bike path.
     * Workflow: update existing (in-place, auto-persisted) → process new obstacles
     * (validate, geocode, create, save, add, recalculate positions).
     * @param bikePath bike path to update (must have obstacles loaded)
     * @param obstaclesToAdd new obstacles to add (nullable)
     * @param obstaclesToUpdate existing obstacles to modify (nullable)
     * @param routeCoordinates ordered route coordinates
     * @param routeBuffer route buffer (pre-calculated, used only for new obstacles, nullable if none)
     * @param updatedBy user performing update
     * @param updatedAt update timestamp
     * @throws IllegalArgumentException if obstacle ID not found or new obstacle too far
     */
    @Transactional
    public void updateAndSaveObstacles(BikePath bikePath, List<ObstacleCreateRequest> obstaclesToAdd,
                                       List<ObstacleUpdateRequest> obstaclesToUpdate,
                                       List<Coordinate> routeCoordinates, Geometry routeBuffer,
                                       User updatedBy, OffsetDateTime updatedAt) {
        // Update existing obstacles (managed entities - auto-persisted)
        if (obstaclesToUpdate != null && !obstaclesToUpdate.isEmpty())
            updateExistingObstacles(bikePath, obstaclesToUpdate, updatedBy, updatedAt);
        // Create and save new obstacles
        if (obstaclesToAdd != null && !obstaclesToAdd.isEmpty()) {
            validateCreateObstaclesInput(bikePath, routeCoordinates, routeBuffer, updatedBy, updatedAt);
            processNewObstacles(obstaclesToAdd, bikePath, routeCoordinates, routeBuffer, updatedBy, updatedAt);
        }
    }

    /**
     * Calculates obstacle component of bike path score using inverted logistic model.
     * Pure function, doesn't modify entities.
     * Algorithm: filter active → calculate average severity → calculate impact density →
     * apply logistic model: MAX_SCORE / (1 + impact/k).
     * Result range: no obstacles = MAX_SCORE, high impact → 0 (asymptotic).
     * @param obstacles list of obstacles
     * @param totalDistance bike path distance in km
     * @return obstacle score in [0.0, MAX_SCORE] with 2 decimal precision
     */
    public BigDecimal calculateObstacleScore(List<Obstacle> obstacles, BigDecimal totalDistance) {
        // Filter active obstacles
        List<Obstacle> activeObstacles = obstacles.stream()
                .filter(Obstacle::getActive)
                .toList();
        if (activeObstacles.isEmpty())
            return BigDecimal.valueOf(MAX_SCORE).setScale(2, RoundingMode.HALF_UP);
        // Calculate impact and apply logistic model
        double avgSeverity = calculateAverageSeverity(activeObstacles);
        double distance = Math.max(totalDistance.doubleValue(), MIN_DISTANCE_KM);
        double impact = calculateImpactDensity(activeObstacles.size(), avgSeverity, distance);
        double obstacleScore = applyLogisticModel(impact);
        return BigDecimal.valueOf(obstacleScore).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Recalculates positions for all obstacles and sorts them.
     * Uses SpatialService to project obstacles onto route and assign 1-indexed positions.
     * Replaces temporary positions (0) with correct final positions.
     * Changes auto-persisted via JPA dirty checking.
     * @param bikePath bike path containing obstacles
     * @param routeCoordinates ordered route coordinates
     */
    private void recalculateAllObstaclePositions(BikePath bikePath, List<Coordinate> routeCoordinates) {
        List<Obstacle> allObstacles = new ArrayList<>(bikePath.getObstacles());
        if (allObstacles.isEmpty())
            return;
        // Create map of obstacle ID → coordinate (O(n) optimization)
        Map<Long, Coordinate> obstacleCoordinateMap = allObstacles.stream()
                .collect(Collectors.toMap(
                        Obstacle::getId,
                        obstacle -> Coordinate.toCoordinate(
                                obstacle.getLatitude(),
                                obstacle.getLongitude()
                        )
                ));
        // Calculate positions via SpatialService
        Map<Long, Integer> obstaclePositions = spatialService.orderObstaclesAlongRoute(
                routeCoordinates,
                obstacleCoordinateMap
        );
        // Update positions (auto-persisted)
        for (Obstacle obstacle : allObstacles) {
            Integer position = obstaclePositions.get(obstacle.getId());
            obstacle.setPositionOnPath(position);
        }
        sortObstaclesByPositionOnPath(bikePath.getObstacles());
    }

    /**
     * Sorts obstacles collection in memory by position on path.
     * Ensures consistency with entity ordering after in-memory modifications.
     * Database OrderBy annotation only applies when loading, so explicit sort needed after updates.
     * @param obstacles list of obstacles to sort (modified in place)
     */
    private void sortObstaclesByPositionOnPath(List<Obstacle> obstacles) {
        if (obstacles != null && !obstacles.isEmpty())
            obstacles.sort(Comparator.comparing(Obstacle::getPositionOnPath));
    }

    /**
     * Complete workflow for adding new obstacles.
     * Geocodes, validates, creates, saves, adds to bikePath, and recalculates positions.
     * Eliminates duplication between createAndSaveObstacles and updateAndSaveObstacles.
     * @param requests obstacle creation requests
     * @param bikePath bike path entity (must be saved with ID)
     * @param routeCoordinates ordered route coordinates
     * @param routeBuffer geometric buffer around route
     * @param user user creating/updating obstacles
     * @param timestamp creation/update timestamp
     */
    private void processNewObstacles(List<ObstacleCreateRequest> requests,
                                     BikePath bikePath,
                                     List<Coordinate> routeCoordinates,
                                     Geometry routeBuffer,
                                     User user,
                                     OffsetDateTime timestamp) {
        // Geocode all addresses
        List<String> addresses = requests.stream()
                .map(ObstacleCreateRequest::address)
                .toList();
        List<GeocodeResult> geocodeResults = mapboxService.geocodeAddresses(addresses);
        // Validate proximity to route
        validateObstaclesProximity(geocodeResults, routeBuffer);
        // Create entities with temporary positions
        List<Obstacle> obstacles = obstacleMapper.toEntities(
                requests,
                geocodeResults,
                bikePath,
                user,
                timestamp,
                null,
                null,
                DEFAULT_ACTIVE_STATUS,
                Collections.nCopies(requests.size(), TEMPORARY_POSITION)
        );
        // Batch save to database
        List<Obstacle> savedObstacles = obstacleRepository.saveAll(obstacles);
        // Add to bikePath collection
        savedObstacles.forEach(bikePath::addObstacle);
        // Recalculate all positions
        recalculateAllObstaclePositions(bikePath, routeCoordinates);
    }

    /**
     * Validates input parameters for creating obstacles.
     * @param bikePath bike path (must not be null)
     * @param routeCoordinates route coordinates (must not be null or empty)
     * @param routeBuffer route buffer (must not be null)
     * @param createdBy user creating obstacles (must not be null)
     * @param createdAt creation timestamp (must not be null)
     * @throws IllegalArgumentException if any parameter null or invalid
     */
    private void validateCreateObstaclesInput(BikePath bikePath, List<Coordinate> routeCoordinates,
                                              Geometry routeBuffer, User createdBy, OffsetDateTime createdAt) {
        if (bikePath == null)
            throw new IllegalArgumentException("BikePath cannot be null");
        if (routeCoordinates == null || routeCoordinates.isEmpty())
            throw new IllegalArgumentException("Route coordinates cannot be null or empty");
        if (routeBuffer == null)
            throw new IllegalArgumentException("Route buffer cannot be null");
        if (createdBy == null)
            throw new IllegalArgumentException("User (createdBy) cannot be null");
        if (createdAt == null)
            throw new IllegalArgumentException("Creation timestamp (createdAt) cannot be null");
    }

    /**
     * Validates all obstacles are within route buffer using SpatialService.
     * Ensures reported obstacles are actually on or near bike path.
     * @param geocodeResults geocoded obstacle coordinates
     * @param routeBuffer geometric buffer around route
     * @throws ObstacleTooFarException if any obstacle outside buffer
     */
    private void validateObstaclesProximity(List<GeocodeResult> geocodeResults, Geometry routeBuffer) {
        geocodeResults.stream()
                .filter(result -> !spatialService.isCoordinateInGeometry(result.coordinate(), routeBuffer))
                .findFirst()
                .ifPresent(result -> {
                    throw new ObstacleTooFarException(
                            "Obstacle at address '" + result.address() + "' is too far from the bike path route"
                    );
                });
    }

    /**
     * Validates all obstacles to update belong to specified bike path.
     * @param bikePath bike path containing obstacles
     * @param updates list of obstacle update requests
     * @throws ObstacleNotFoundException if obstacle ID not found in bike path
     */
    private void validateObstaclesBelongToBikePath(BikePath bikePath, List<ObstacleUpdateRequest> updates) {
        Map<Long, Obstacle> obstacleMap = bikePath.getObstacles().stream()
                .collect(Collectors.toMap(Obstacle::getId, Function.identity()));
        for (ObstacleUpdateRequest update : updates) {
            if (!obstacleMap.containsKey(update.id()))
                throw new ObstacleNotFoundException("Obstacle with ID " + update.id() + " not found in this bike path");
        }
    }

    /**
     * Calculates average severity level across obstacles.
     * @param obstacles list of obstacles (must not be empty)
     * @return average severity level, or 0.0 if empty
     */
    private double calculateAverageSeverity(List<Obstacle> obstacles) {
        return obstacles.stream()
                .mapToInt(obstacle -> obstacle.getSeverity().getSeverityLevel())
                .average()
                .orElse(0.0);
    }

    /**
     * Calculates impact density for obstacle score.
     * Formula: (count × avgSeverity) / distance
     * @param obstacleCount number of active obstacles
     * @param avgSeverity average severity level
     * @param distance bike path distance in km (must be > 0)
     * @return impact density value
     */
    private double calculateImpactDensity(int obstacleCount, double avgSeverity, double distance) {
        return (obstacleCount * avgSeverity) / distance;
    }

    /**
     * Applies inverted logistic model to calculate obstacle score.
     * Formula: MAX_SCORE / (1 + impact/k)
     * Creates asymptotic penalty: low impact → MAX_SCORE, high impact → 0.
     * @param impact calculated impact density
     * @return obstacle score in [0, MAX_SCORE]
     */
    private double applyLogisticModel(double impact) {
        return MAX_SCORE / (1.0 + (impact / K));
    }

    /**
     * Updates fields of existing obstacles (partial update).
     * Only non-null fields modified. Changes auto-persisted via JPA dirty checking.
     * @param bikePath bike path containing obstacles
     * @param updates list of obstacle update requests
     * @param updatedBy user performing update
     * @param updatedAt update timestamp
     * @throws ObstacleNotFoundException if obstacle ID not found
     */
    private void updateExistingObstacles(BikePath bikePath, List<ObstacleUpdateRequest> updates,
                                         User updatedBy, OffsetDateTime updatedAt) {
        validateObstaclesBelongToBikePath(bikePath, updates);
        // Create map for efficient lookup
        Map<Long, Obstacle> obstacleMap = bikePath.getObstacles().stream()
                .collect(Collectors.toMap(Obstacle::getId, Function.identity()));
        // Process updates
        for (ObstacleUpdateRequest update : updates) {
            Obstacle obstacle = obstacleMap.get(update.id());
            // Partial update - only non-null fields
            if (update.type() != null)
                obstacle.setType(update.type());
            if (update.severity() != null)
                obstacle.setSeverity(update.severity());
            if (update.active() != null)
                obstacle.setActive(update.active());
            obstacle.setUpdatedBy(updatedBy);
            obstacle.setUpdatedAt(updatedAt);
        }
    }

}