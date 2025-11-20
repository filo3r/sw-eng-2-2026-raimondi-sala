package it.polimi.se.bbp.service;

import it.polimi.se.bbp.dto.mapbox.Coordinate;
import it.polimi.se.bbp.dto.mapbox.CyclingRouteResult;
import it.polimi.se.bbp.dto.mapbox.GeocodeResult;
import it.polimi.se.bbp.dto.request.BikePathManualCreateRequest;
import it.polimi.se.bbp.entity.BikePath;
import it.polimi.se.bbp.entity.User;
import it.polimi.se.bbp.enums.BikePathStatus;
import it.polimi.se.bbp.mapper.entity.BikePathMapper;
import it.polimi.se.bbp.mapper.entity.BikePathPointMapper;
import it.polimi.se.bbp.repository.BikePathPointRepository;
import it.polimi.se.bbp.repository.BikePathRepository;
import it.polimi.se.bbp.repository.ObstacleRepository;
import it.polimi.se.bbp.repository.UserRepository;
import it.polimi.se.bbp.service.mapbox.MapboxService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.GeometryFactory;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BikePathServiceTest {

    @Mock private BikePathRepository bikePathRepository;
    @Mock private UserRepository userRepository;
    @Mock private BikePathPointRepository bikePathPointRepository;
    @Mock private ObstacleRepository obstacleRepository;
    @Mock private MapboxService mapboxService;
    @Mock private ObstacleService obstacleService;
    @Spy private GeometryFactory geometryFactory = new GeometryFactory();
    @Mock private BikePathMapper bikePathMapper;
    @Mock private BikePathPointMapper bikePathPointMapper;
    @Mock private EntityManager entityManager;

    @InjectMocks
    private BikePathService bikePathService;

    @BeforeEach
    void setupSecurity() {
        Authentication auth = mock(Authentication.class);
        SecurityContext ctx = mock(SecurityContext.class);
        lenient().when(ctx.getAuthentication()).thenReturn(auth);
        lenient().when(auth.getPrincipal()).thenReturn(1L);
        SecurityContextHolder.setContext(ctx);
    }

    @Test
    void createBikePathManual_happyPath() {
        // inputs
        BikePathManualCreateRequest req = new BikePathManualCreateRequest();
        req.setAddresses(List.of("A", "B"));
        req.setObstacles(List.of());

        // mocks
        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // geocoding
        GeocodeResult geo = new GeocodeResult();
        geo.setCoordinate(new Coordinate(10.0, 10.0));
        when(mapboxService.geocodeAddressesParallel(any())).thenReturn(List.of(geo, geo));

        // routing
        CyclingRouteResult route = new CyclingRouteResult();
        route.setRouteCoordinates(List.of(
            new Coordinate(10.0, 10.0),
            new Coordinate(10.1, 10.1)
        ));
        route.setDistanceInMeters(1000.0);
        when(mapboxService.calculateCyclingRoute(any())).thenReturn(route);
        when(obstacleService.calculateObstacleScore(any(), any())).thenReturn(BigDecimal.valueOf(5.0));

        // entities
        BikePath bp = new BikePath();
        bp.setId(100L);
        bp.setStatus(BikePathStatus.EXCELLENT);
        bp.setTotalDistance(BigDecimal.ONE);
        
        when(bikePathMapper.toEntity(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(bp);
        when(bikePathRepository.save(any())).thenReturn(bp);
        when(bikePathRepository.findByIdWithPoints(100L)).thenReturn(Optional.of(bp));

        // execute
        BikePath result = bikePathService.createBikePathManual(req);

        // verify
        assertNotNull(result);
        verify(bikePathPointRepository).saveAll(any());
        verify(obstacleService).calculateObstacleScore(any(), any());
    }
}