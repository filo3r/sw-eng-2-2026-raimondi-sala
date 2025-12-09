package it.polimi.se.bbp.service;

import it.polimi.se.bbp.dto.mapbox.Coordinate;
import it.polimi.se.bbp.dto.mapbox.GeocodeResult;
import it.polimi.se.bbp.dto.request.ObstacleCreateRequest;
import it.polimi.se.bbp.entity.BikePath;
import it.polimi.se.bbp.entity.Obstacle;
import it.polimi.se.bbp.entity.User;
import it.polimi.se.bbp.enums.ObstacleSeverity;
import it.polimi.se.bbp.enums.ObstacleType;
import it.polimi.se.bbp.mapper.entity.ObstacleMapper;
import it.polimi.se.bbp.repository.ObstacleRepository;
import it.polimi.se.bbp.service.mapbox.MapboxService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ObstacleServiceTest {

    @Mock private MapboxService mapboxService;
    @Mock private ObstacleMapper obstacleMapper;
    @Mock private ObstacleRepository obstacleRepository;
    @Mock private EntityManager entityManager;
    @Spy private GeometryFactory geometryFactory = new GeometryFactory();

    @InjectMocks
    private ObstacleService obstacleService;

    private Geometry mockRouteBuffer;

    @BeforeEach
    void setUp() {
        // create a simple buffer around 0,0
        Point p = geometryFactory.createPoint(new org.locationtech.jts.geom.Coordinate(0, 0));
        mockRouteBuffer = p.buffer(1.0);
    }

    @Test
    void createObstacles_validatesProximity_success() {
        // setup
        ObstacleCreateRequest req = new ObstacleCreateRequest();
        req.setAddress("valid loc");
        
        GeocodeResult geoRes = new GeocodeResult();
        geoRes.setCoordinate(new Coordinate(0.0, 0.0)); // inside buffer
        
        when(mapboxService.geocodeAddressesParallel(any())).thenReturn(List.of(geoRes));
        when(obstacleMapper.toEntity(any(), any(), any(), any(), any(), any(), any(), anyBoolean()))
                .thenReturn(new Obstacle());

        // execute
        List<Obstacle> result = obstacleService.createObstacles(
                List.of(req), new BikePath(), mockRouteBuffer, new User(), OffsetDateTime.now()
        );

        // verify
        assertFalse(result.isEmpty());
    }

    @Test
    void createObstacles_throwsWhenOutsideBuffer() {
        // setup
        ObstacleCreateRequest req = new ObstacleCreateRequest();
        req.setAddress("far away");

        GeocodeResult geoRes = new GeocodeResult();
        geoRes.setCoordinate(new Coordinate(50.0, 50.0)); // outside buffer

        when(mapboxService.geocodeAddressesParallel(any())).thenReturn(List.of(geoRes));

        // execute and verify
        assertThrows(IllegalArgumentException.class, () -> {
            obstacleService.createObstacles(
                    List.of(req), new BikePath(), mockRouteBuffer, new User(), OffsetDateTime.now()
            );
        });
    }

    @Test
    void calculateObstacleScore_logicCheck() {
        // setup obstacles
        Obstacle o1 = Obstacle.builder().active(true).severity(ObstacleSeverity.MEDIUM).build(); // level 2
        BigDecimal distance = BigDecimal.valueOf(1.0); // 1 km

        BigDecimal score = obstacleService.calculateObstacleScore(List.of(o1), distance);

        assertEquals(0, BigDecimal.valueOf(2.50).compareTo(score));
    }
}