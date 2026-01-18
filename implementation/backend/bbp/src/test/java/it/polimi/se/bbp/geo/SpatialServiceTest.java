package it.polimi.se.bbp.geo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Geometry;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Spatial Service Tests")
class SpatialServiceTest {

    @InjectMocks
    private SpatialService spatialService;

    private List<Coordinate> mockRoute;

    @BeforeEach
    void setUp() {
        // Initialize a simple route
        mockRoute = new ArrayList<>();
        mockRoute.add(new Coordinate(0.0, 0.0));
        mockRoute.add(new Coordinate(0.0, 1.0));
    }

    @Test
    @DisplayName("Should create a geometric buffer around a valid route")
    void createRouteBuffer_Success() {
        double bufferDistanceMeters = 10.0;

        Geometry result = spatialService.createRouteBuffer(mockRoute, bufferDistanceMeters);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.isValid());
        // The buffer of a line string should be a Polygon
        assertEquals("Polygon", result.getGeometryType());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when creating buffer for null or insufficient route")
    void createRouteBuffer_ThrowsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () ->
                spatialService.createRouteBuffer(null, 10.0));

        assertThrows(IllegalArgumentException.class, () ->
                spatialService.createRouteBuffer(List.of(new Coordinate(0.0, 0.0)), 10.0));
    }

    @Test
    @DisplayName("Should return true when coordinate is inside the geometry")
    void isCoordinateInGeometry_ReturnsTrueForPointInside() {
        // Create a buffer around the route
        Geometry buffer = spatialService.createRouteBuffer(mockRoute, 50.0);

        // Point on the line (0.0, 0.5)
        Coordinate pointOnLine = new Coordinate(0.0, 0.5);

        assertTrue(spatialService.isCoordinateInGeometry(pointOnLine, buffer));
    }

    @Test
    @DisplayName("Should return false when coordinate is outside the geometry")
    void isCoordinateInGeometry_ReturnsFalseForPointOutside() {
        // Create a small buffer around the route (approx 10 meters)
        Geometry buffer = spatialService.createRouteBuffer(mockRoute, 10.0);

        // Point far away (1.0, 1.0)
        Coordinate pointOutside = new Coordinate(1.0, 1.0);

        assertFalse(spatialService.isCoordinateInGeometry(pointOutside, buffer));
    }

    @Test
    @DisplayName("Should handle null inputs gracefully for containment check")
    void isCoordinateInGeometry_ReturnsFalseForNulls() {
        Geometry buffer = spatialService.createRouteBuffer(mockRoute, 10.0);
        Coordinate coord = new Coordinate(0.0, 0.0);

        assertFalse(spatialService.isCoordinateInGeometry(null, buffer));
        assertFalse(spatialService.isCoordinateInGeometry(coord, null));
    }

    @Test
    @DisplayName("Should correctly order obstacles along the route")
    void orderObstaclesAlongRoute_Success() {
        // Route is (0,0) -> (0,1)

        // Define obstacles
        Map<Long, Coordinate> obstacles = new HashMap<>();
        // Obstacle 1: near start (0, 0.1)
        obstacles.put(100L, new Coordinate(0.0, 0.1));
        // Obstacle 2: near end (0, 0.9)
        obstacles.put(200L, new Coordinate(0.0, 0.9));
        // Obstacle 3: in middle (0, 0.5)
        obstacles.put(300L, new Coordinate(0.0, 0.5));

        Map<Long, Integer> result = spatialService.orderObstaclesAlongRoute(mockRoute, obstacles);

        assertNotNull(result);
        assertEquals(3, result.size());

        // Expected order:
        // 1. Obstacle 100L (at 0.1) -> Position 1
        // 2. Obstacle 300L (at 0.5) -> Position 2
        // 3. Obstacle 200L (at 0.9) -> Position 3

        assertEquals(1, result.get(100L));
        assertEquals(2, result.get(300L));
        assertEquals(3, result.get(200L));
    }

    @Test
    @DisplayName("Should project obstacles not exactly on the line to the closest point for ordering")
    void orderObstaclesAlongRoute_ProjectsPoints() {
        // Route is (0,0) -> (0,1)

        Map<Long, Coordinate> obstacles = new HashMap<>();
        // Obstacle A: at (0.01, 0.2) -> slightly off line, at 0.2 linear dist
        obstacles.put(1L, new Coordinate(0.01, 0.2));
        // Obstacle B: at (-0.01, 0.8) -> slightly off line, at 0.8 linear dist
        obstacles.put(2L, new Coordinate(-0.01, 0.8));

        Map<Long, Integer> result = spatialService.orderObstaclesAlongRoute(mockRoute, obstacles);

        assertEquals(1, result.get(1L));
        assertEquals(2, result.get(2L));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when ordering with invalid route or null obstacles")
    void orderObstaclesAlongRoute_ThrowsInvalidInput() {
        Map<Long, Coordinate> obstacles = new HashMap<>();
        obstacles.put(1L, new Coordinate(0.0, 0.0));

        assertThrows(IllegalArgumentException.class, () ->
                spatialService.orderObstaclesAlongRoute(null, obstacles));

        assertThrows(IllegalArgumentException.class, () ->
                spatialService.orderObstaclesAlongRoute(List.of(new Coordinate(0.0, 0.0)), obstacles));

        assertThrows(IllegalArgumentException.class, () ->
                spatialService.orderObstaclesAlongRoute(mockRoute, null));
    }
}