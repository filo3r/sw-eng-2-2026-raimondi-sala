package it.polimi.se.bbp.service;

import it.polimi.se.bbp.dto.request.ObstacleCreateRequest;
import it.polimi.se.bbp.dto.request.ObstacleUpdateRequest;
import it.polimi.se.bbp.dto.result.GeocodeResult;
import it.polimi.se.bbp.entity.BikePath;
import it.polimi.se.bbp.entity.Obstacle;
import it.polimi.se.bbp.entity.User;
import it.polimi.se.bbp.enums.ObstacleSeverity;
import it.polimi.se.bbp.enums.ObstacleType;
import it.polimi.se.bbp.exception.obstacle.ObstacleNotFoundException;
import it.polimi.se.bbp.exception.obstacle.ObstacleTooFarException;
import it.polimi.se.bbp.geo.Coordinate;
import it.polimi.se.bbp.geo.SpatialService;
import it.polimi.se.bbp.mapper.entity.ObstacleMapper;
import it.polimi.se.bbp.repository.ObstacleRepository;
import it.polimi.se.bbp.service.mapbox.MapboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Geometry;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Obstacle Service Tests")
class ObstacleServiceTest {

    @Mock
    private ObstacleRepository obstacleRepository;
    @Mock
    private MapboxService mapboxService;
    @Mock
    private SpatialService spatialService;
    @Mock
    private ObstacleMapper obstacleMapper;
    @Mock
    private Geometry routeBuffer;

    @InjectMocks
    private ObstacleService obstacleService;

    private User user;
    private BikePath bikePath;
    private List<Coordinate> routeCoordinates;
    private OffsetDateTime now;

    @BeforeEach
    void setUp() {
        // Initialize the test data, setting up a user, a bike path, and route coordinates
        user = User.builder().id(1L).username("testuser").build();

        bikePath = BikePath.builder()
                .id(100L)
                .obstacles(new ArrayList<>())
                .build();

        routeCoordinates = List.of(new Coordinate(45.0, 9.0), new Coordinate(45.1, 9.1));
        now = OffsetDateTime.now();
    }

    @Test
    @DisplayName("Should create and save obstacles successfully when valid")
    void createAndSaveObstacles_Success() {
        // We simulate a successful flow where an obstacle address is geocoded, validated within the route buffer, and saved
        ObstacleCreateRequest req = new ObstacleCreateRequest("Via Venezia 1", ObstacleType.POTHOLE, ObstacleSeverity.MEDIUM);
        List<ObstacleCreateRequest> requests = List.of(req);

        GeocodeResult geoResult = new GeocodeResult("Via Venezia 1", new Coordinate(45.05, 9.05));
        List<GeocodeResult> geoResults = List.of(geoResult);

        Obstacle newObstacle = Obstacle.builder().id(1L).build();
        List<Obstacle> obstacles = List.of(newObstacle);

        when(mapboxService.geocodeAddresses(anyList())).thenReturn(geoResults);

        when(spatialService.isCoordinateInGeometry(any(), eq(routeBuffer))).thenReturn(true);

        when(obstacleMapper.toEntities(any(), any(), any(), any(), any(), any(), any(), anyBoolean(), any()))
                .thenReturn(obstacles);
        when(obstacleRepository.saveAll(anyList())).thenReturn(obstacles);

        when(spatialService.orderObstaclesAlongRoute(anyList(), anyMap())).thenReturn(Map.of(1L, 1));

        obstacleService.createAndSaveObstacles(requests, bikePath, routeCoordinates, routeBuffer, user, now);

        verify(mapboxService).geocodeAddresses(anyList());
        verify(spatialService).isCoordinateInGeometry(any(), eq(routeBuffer));
        verify(obstacleRepository).saveAll(obstacles);
        verify(spatialService).orderObstaclesAlongRoute(eq(routeCoordinates), anyMap());

        // Verify obstacle was added to bike path and positioned correctly
        assertEquals(1, bikePath.getObstacles().size());
        assertEquals(1, bikePath.getObstacles().get(0).getPositionOnPath());
    }

    @Test
    @DisplayName("Should throw ObstacleTooFarException when obstacle is outside buffer")
    void createAndSaveObstacles_TooFar_ThrowsException() {
        // If the geocoded coordinate lies outside the calculated route buffer, the service must reject the creation
        ObstacleCreateRequest req = new ObstacleCreateRequest("Via Milano 1", ObstacleType.POTHOLE, ObstacleSeverity.HIGH);
        List<ObstacleCreateRequest> requests = List.of(req);

        GeocodeResult geoResult = new GeocodeResult("Via Milano 1", new Coordinate(10.0, 10.0));

        when(mapboxService.geocodeAddresses(anyList())).thenReturn(List.of(geoResult));
        when(spatialService.isCoordinateInGeometry(any(), eq(routeBuffer))).thenReturn(false);

        assertThrows(ObstacleTooFarException.class, () ->
                obstacleService.createAndSaveObstacles(requests, bikePath, routeCoordinates, routeBuffer, user, now)
        );

        verify(obstacleRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Should do nothing if request list is empty")
    void createAndSaveObstacles_EmptyList_DoNothing() {
        // If the request list is null or empty, the service should immediately return without performing API calls
        obstacleService.createAndSaveObstacles(null, bikePath, routeCoordinates, routeBuffer, user, now);
        obstacleService.createAndSaveObstacles(List.of(), bikePath, routeCoordinates, routeBuffer, user, now);

        verify(mapboxService, never()).geocodeAddresses(anyList());
    }

    @Test
    @DisplayName("Should update existing obstacles successfully")
    void updateAndSaveObstacles_UpdateExisting_Success() {
        // We simulate updating an existing obstacle's details without triggering new geocoding
        Obstacle existingObstacle = Obstacle.builder()
                .id(50L)
                .type(ObstacleType.POTHOLE)
                .severity(ObstacleSeverity.LOW)
                .active(true)
                .build();
        bikePath.addObstacle(existingObstacle);

        ObstacleUpdateRequest updateReq = new ObstacleUpdateRequest(
                50L, ObstacleType.CONSTRUCTION, ObstacleSeverity.HIGH, false
        );

        obstacleService.updateAndSaveObstacles(bikePath, null, List.of(updateReq), routeCoordinates, routeBuffer, user, now);

        assertEquals(ObstacleType.CONSTRUCTION, existingObstacle.getType());
        assertEquals(ObstacleSeverity.HIGH, existingObstacle.getSeverity());
        assertFalse(existingObstacle.getActive());
        assertEquals(user, existingObstacle.getUpdatedBy());

        verify(mapboxService, never()).geocodeAddresses(anyList());
    }

    @Test
    @DisplayName("Should throw ObstacleNotFoundException when updating obstacle not in bike path")
    void updateAndSaveObstacles_NotFound_ThrowsException() {
        // If the request attempts to update an obstacle ID that is not associated with the current bike path, an exception is expected
        ObstacleUpdateRequest updateReq = new ObstacleUpdateRequest(
                999L, null, null, null
        );

        assertThrows(ObstacleNotFoundException.class, () ->
                obstacleService.updateAndSaveObstacles(bikePath, null, List.of(updateReq), routeCoordinates, routeBuffer, user, now)
        );
    }

    @Test
    @DisplayName("Should add new obstacles during update")
    void updateAndSaveObstacles_AddNew_Success() {
        // We simulate a scenario where the update request includes new obstacles to be created, triggering the creation flow
        ObstacleCreateRequest addReq = new ObstacleCreateRequest("Via Torino 1", ObstacleType.ICE, ObstacleSeverity.MEDIUM);

        GeocodeResult geoResult = new GeocodeResult("Via Torino 1", new Coordinate(45.0, 9.0));
        Obstacle newEntity = Obstacle.builder().id(2L).build();

        when(mapboxService.geocodeAddresses(anyList())).thenReturn(List.of(geoResult));
        when(spatialService.isCoordinateInGeometry(any(), eq(routeBuffer))).thenReturn(true);
        when(obstacleMapper.toEntities(any(), any(), any(), any(), any(), any(), any(), anyBoolean(), any()))
                .thenReturn(List.of(newEntity));
        when(obstacleRepository.saveAll(anyList())).thenReturn(List.of(newEntity));
        when(spatialService.orderObstaclesAlongRoute(any(), any())).thenReturn(Map.of(2L, 1));

        obstacleService.updateAndSaveObstacles(bikePath, List.of(addReq), null, routeCoordinates, routeBuffer, user, now);

        verify(obstacleRepository).saveAll(anyList());
        assertEquals(1, bikePath.getObstacles().size());
    }

    @Test
    @DisplayName("Should return max score when list is empty")
    void calculateObstacleScore_Empty_ReturnsMax() {
        // If there are no obstacles, the score should default to the maximum value
        BigDecimal score = obstacleService.calculateObstacleScore(List.of(), BigDecimal.TEN);

        assertEquals(BigDecimal.valueOf(5.00).setScale(2), score);
    }

    @Test
    @DisplayName("Should calculate score correctly with active obstacles")
    void calculateObstacleScore_ActiveObstacles_CalculatesCorrectly() {
        // Tests the scoring algorithm ensuring that active obstacles reduce the score based on severity and distance
        Obstacle o1 = Obstacle.builder().active(true).severity(ObstacleSeverity.MEDIUM).build();
        Obstacle o2 = Obstacle.builder().active(true).severity(ObstacleSeverity.HIGH).build();

        List<Obstacle> obstacles = List.of(o1, o2);
        BigDecimal distance = BigDecimal.valueOf(10.0);

        BigDecimal result = obstacleService.calculateObstacleScore(obstacles, distance);

        assertEquals(BigDecimal.valueOf(4.00).setScale(2), result);
    }

    @Test
    @DisplayName("Should ignore inactive obstacles in score calculation")
    void calculateObstacleScore_IgnoreInactive_Success() {
        // Inactive obstacles should be excluded from the score calculation logic
        Obstacle active = Obstacle.builder().active(true).severity(ObstacleSeverity.HIGH).build();
        Obstacle inactive = Obstacle.builder().active(false).severity(ObstacleSeverity.HIGH).build();

        BigDecimal scoreWithInactive = obstacleService.calculateObstacleScore(List.of(active, inactive), BigDecimal.valueOf(10));
        BigDecimal scoreActiveOnly = obstacleService.calculateObstacleScore(List.of(active), BigDecimal.valueOf(10));

        assertEquals(scoreActiveOnly, scoreWithInactive);
    }
}