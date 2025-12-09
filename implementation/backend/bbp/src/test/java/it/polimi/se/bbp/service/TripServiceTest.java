package it.polimi.se.bbp.service;

import it.polimi.se.bbp.dto.mapbox.Coordinate;
import it.polimi.se.bbp.dto.mapbox.CyclingRouteResult;
import it.polimi.se.bbp.dto.mapbox.GeocodeResult;
import it.polimi.se.bbp.dto.request.TripManualRecordRequest;
import it.polimi.se.bbp.entity.Trip;
import it.polimi.se.bbp.entity.User;
import it.polimi.se.bbp.mapper.entity.TripMapper;
import it.polimi.se.bbp.mapper.entity.TripPointMapper;
import it.polimi.se.bbp.repository.TripPointRepository;
import it.polimi.se.bbp.repository.TripRepository;
import it.polimi.se.bbp.repository.UserRepository;
import it.polimi.se.bbp.service.mapbox.MapboxService;
import it.polimi.se.bbp.service.openmeteo.OpenMeteoService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    @Mock private TripRepository tripRepository;
    @Mock private TripPointRepository tripPointRepository;
    @Mock private UserRepository userRepository;
    @Mock private MapboxService mapboxService;
    @Mock private OpenMeteoService openMeteoService;
    @Mock private TripMapper tripMapper;
    @Mock private TripPointMapper tripPointMapper;
    @Mock private EntityManager entityManager;
    @Mock private SecurityContext securityContext;
    @Mock private Authentication authentication;

    @InjectMocks
    private TripService tripService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getPrincipal()).thenReturn(1L);

        mockUser = User.builder().id(1L).email("test@test.com").build();
    }

    @Test
    void shouldRecordTripManualSuccessfully() {
        // prepare data
        TripManualRecordRequest request = TripManualRecordRequest.builder()
                .addresses(List.of("Milano", "Monza"))
                .startTime(OffsetDateTime.now().minusHours(1))
                .endTime(OffsetDateTime.now())
                .build();

        GeocodeResult geo1 = GeocodeResult.builder().address("Milano").coordinate(new Coordinate(45.0, 9.0)).build();
        GeocodeResult geo2 = GeocodeResult.builder().address("Monza").coordinate(new Coordinate(45.5, 9.2)).build();

        CyclingRouteResult routeResult = CyclingRouteResult.builder()
                .distanceInMeters(15000.0)
                .routeCoordinates(List.of(new Coordinate(45.0, 9.0), new Coordinate(45.5, 9.2)))
                .build();

        Trip mockTrip = Trip.builder().id(100L).recordedBy(mockUser).build();

        // mock behaviour
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(mapboxService.geocodeAddressesParallel(anyList())).thenReturn(List.of(geo1, geo2));
        when(mapboxService.calculateCyclingRoute(anyList())).thenReturn(routeResult);

        // mock mapper and save
        when(tripMapper.toEntity(any(), any(), any(), any(), anyInt(), any(), any(), any()))
                .thenReturn(mockTrip);
        when(tripRepository.save(any(Trip.class))).thenReturn(mockTrip);
        when(tripRepository.findByIdWithPointsAndWeather(100L)).thenReturn(Optional.of(mockTrip));

        // execute registration
        Trip result = tripService.recordTripManual(request);

        // check
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);

        verify(mapboxService).geocodeAddressesParallel(request.getAddresses());
        verify(mapboxService).calculateCyclingRoute(anyList());
        verify(tripRepository).save(any(Trip.class));
        verify(tripPointRepository).saveAll(anyList());
    }
}