package it.polimi.se.bbp.geo;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.linearref.LinearLocation;
import org.locationtech.jts.linearref.LocationIndexedLine;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for advanced spatial operations using JTS (Java Topology Suite).
 * Provides geometry creation, buffering, and spatial relationship checks.
 * Isolates business logic from geometry library implementation.
 */
@Service
public class SpatialService {

    private final GeometryFactory geometryFactory;

    /**
     * Initializes GeometryFactory with floating-point precision.
     * SRID 4326 (WGS84) is used as standard for GPS coordinates.
     */
    public SpatialService() {
        this.geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    }

    /**
     * Creates geometric buffer around path defined by coordinates.
     * Useful for defining area of influence around route (e.g., checking nearby obstacles).
     * @param path list of coordinates defining the route
     * @param bufferDistanceMeters distance in meters for buffer radius
     * @return JTS Geometry representing buffered area (Polygon or MultiPolygon)
     * @throws IllegalArgumentException if path is null or has fewer than 2 coordinates
     */
    public Geometry createRouteBuffer(List<Coordinate> path, double bufferDistanceMeters) {
        if (path == null || path.size() < 2)
            throw new IllegalArgumentException("A route must consist of at least 2 coordinates to create a buffer.");
        // 1. Convert domain coordinates to JTS Coordinates
        org.locationtech.jts.geom.Coordinate[] jtsCoordinates = path.stream()
                .map(Coordinate::toJtsCoordinate)
                .toArray(org.locationtech.jts.geom.Coordinate[]::new);
        // 2. Create the LineString (the route spine)
        LineString routeLine = geometryFactory.createLineString(jtsCoordinates);
        // 3. Convert meters to degrees for buffering
        // We calculate the average latitude to get a reasonable approximation for the degree conversion
        double averageLat = path.getFirst().getLatitude(); // Simplified: using start point is usually sufficient for local buffers
        double bufferDegrees = GeoUtils.metersToDegrees(bufferDistanceMeters, averageLat);
        // 4. Generate the buffer geometry
        return routeLine.buffer(bufferDegrees);
    }

    /**
     * Checks if coordinate is contained within geometry.
     * @param coordinate point to check
     * @param geometry geometry area (e.g., route buffer)
     * @return true if point is within or on boundary of geometry
     */
    public boolean isCoordinateInGeometry(Coordinate coordinate, Geometry geometry) {
        if (coordinate == null || geometry == null)
            return false;
        Point point = geometryFactory.createPoint(coordinate.toJtsCoordinate());
        return geometry.contains(point);
    }

    /**
     * Orders obstacles along bike path route based on their position.
     * Uses JTS LinearLocation to project each obstacle onto route.
     * Algorithm: Create LineString, project obstacles onto line, sort by fractional distance,
     * assign sequential positions (1-indexed).
     * @param routeCoordinates list of coordinates defining bike path route in sequential order
     * @param obstacleCoordinates map of obstacle IDs to their coordinates
     * @return map of obstacle IDs to their position on path (1-indexed)
     * @throws IllegalArgumentException if route has fewer than 2 coordinates or obstacleCoordinates is null
     */
    public Map<Long, Integer> orderObstaclesAlongRoute(List<Coordinate> routeCoordinates,
                                                       Map<Long, Coordinate> obstacleCoordinates) {
        // Validation
        if (routeCoordinates == null || routeCoordinates.size() < 2)
            throw new IllegalArgumentException("Route must have at least 2 coordinates");
        if (obstacleCoordinates == null)
            throw new IllegalArgumentException("Obstacle coordinates map cannot be null");
        // Handle empty obstacles case
        if (obstacleCoordinates.isEmpty())
            return new HashMap<>();
        // Step 1: Create LineString from route coordinates
        org.locationtech.jts.geom.Coordinate[] jtsCoordinates = routeCoordinates.stream()
                .map(Coordinate::toJtsCoordinate)
                .toArray(org.locationtech.jts.geom.Coordinate[]::new);
        LineString routeLine = geometryFactory.createLineString(jtsCoordinates);
        // Step 2: Create LocationIndexedLine for linear referencing
        LocationIndexedLine indexedLine = new LocationIndexedLine(routeLine);
        // Step 3: Project each obstacle onto the line and calculate its linear location
        List<ObstaclePosition> obstaclePositions = new ArrayList<>();
        for (Map.Entry<Long, Coordinate> entry : obstacleCoordinates.entrySet()) {
            Long obstacleId = entry.getKey();
            Coordinate obstacleCoord = entry.getValue();
            // Convert to JTS coordinate
            org.locationtech.jts.geom.Coordinate jtsObstacleCoord = obstacleCoord.toJtsCoordinate();
            // Project the obstacle point onto the line to find the closest point on the route
            LinearLocation location = indexedLine.project(jtsObstacleCoord);
            // Get the fractional distance along the line (0.0 = start, 1.0 = end)
            double fractionAlongLine = location.getSegmentFraction() + location.getSegmentIndex();
            obstaclePositions.add(new ObstaclePosition(obstacleId, fractionAlongLine));
        }
        // Step 4: Sort obstacles by their position along the line
        obstaclePositions.sort(Comparator.comparingDouble(ObstaclePosition::fractionAlongLine));
        // Step 5: Assign sequential positions (1-indexed)
        Map<Long, Integer> positionMap = new HashMap<>();
        for (int i = 0; i < obstaclePositions.size(); i++) {
            Long obstacleId = obstaclePositions.get(i).obstacleId();
            int position = i + 1; // 1-indexed position
            positionMap.put(obstacleId, position);
        }
        return positionMap;
    }

    /**
     * Helper record to store obstacle position data during sorting.
     * @param obstacleId ID of the obstacle from database
     * @param fractionAlongLine fractional distance along the line
     */
    private record ObstaclePosition(Long obstacleId, double fractionAlongLine) {}

}