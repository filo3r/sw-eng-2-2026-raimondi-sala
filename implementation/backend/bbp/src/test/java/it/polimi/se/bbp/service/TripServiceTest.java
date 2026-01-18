package it.polimi.se.bbp.service;

import it.polimi.se.bbp.dto.request.TripManualRecordRequest;
import it.polimi.se.bbp.dto.request.TripSearchRequest;
import it.polimi.se.bbp.dto.result.CyclingRouteResult;
import it.polimi.se.bbp.dto.result.GeocodeResult;
import it.polimi.se.bbp.entity.MeteorologicalData;
import it.polimi.se.bbp.entity.Trip;
import it.polimi.se.bbp.entity.User;
import it.polimi.se.bbp.geo.Coordinate;
import it.polimi.se.bbp.mapper.entity.TripMapper;
import it.polimi.se.bbp.mapper.entity.TripPointMapper;
import it.polimi.se.bbp.repository.TripPointRepository;
import it.polimi.se.bbp.repository.TripRepository;
import it.polimi.se.bbp.service.mapbox.MapboxService;
import it.polimi.se.bbp.service.openmeteo.OpenMeteoService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Trip Service Tests")
class TripServiceTest {

    @Mock
    private TripRepository tripRepository;
    @Mock
    private TripPointRepository tripPointRepository;
    @Mock
    private UserAuthService userAuthService;
    @Mock
    private MapboxService mapboxService;
    @Mock
    private OpenMeteoService openMeteoService;
    @Mock
    private TripMapper tripMapper;
    @Mock
    private TripPointMapper tripPointMapper;

    @InjectMocks
    private TripService tripService;

    private User currentUser;
    private Trip userTrip;

    @BeforeEach
    void setUp() {
        // Initialize the base user
        currentUser = User.builder()
                .id(1L)
                .name("Mario")
                .surname("Rossi")
                .username("testuser")
                .build();

        // Initialize a trip recorded by the current user
        userTrip = Trip.builder()
                .id(100L)
                .recordedBy(currentUser)
                .origin("Milan")
                .destination("Monza")
                .startTime(OffsetDateTime.now().minusHours(1))
                .endTime(OffsetDateTime.now())
                .totalDuration(60)
                .totalDistance(BigDecimal.valueOf(15.5))
                .averageSpeed(BigDecimal.valueOf(15.5))
                .tripPoints(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("Should record trip manually when valid data is provided")
    void recordTripManually_Success() {
        // Prepare request
        List<String> addresses = List.of("Milan", "Monza");
        OffsetDateTime start = OffsetDateTime.now().minusHours(1);
        OffsetDateTime end = OffsetDateTime.now();
        TripManualRecordRequest request = new TripManualRecordRequest(
                addresses, "Commute", start, end, BigDecimal.valueOf(20.0)
        );

        // Prepare Geocode and Route results
        GeocodeResult geoRes1 = new GeocodeResult("Milan", new Coordinate(45.46, 9.19));
        GeocodeResult geoRes2 = new GeocodeResult("Monza", new Coordinate(45.58, 9.27));
        List<GeocodeResult> geocodeResults = List.of(geoRes1, geoRes2);

        List<Coordinate> routeCoords = List.of(new Coordinate(45.46, 9.19), new Coordinate(45.58, 9.27));
        CyclingRouteResult routeResult = new CyclingRouteResult(routeCoords, 15500.0);

        // Mock dependencies
        when(userAuthService.getAuthenticatedUser()).thenReturn(currentUser);
        when(mapboxService.geocodeAddresses(addresses)).thenReturn(geocodeResults);
        when(mapboxService.calculateCyclingRoute(any())).thenReturn(routeResult);

        // Mock Mapper
        when(tripMapper.toEntity(any(), any(), any(), any(), anyInt(), any(), any(), any()))
                .thenReturn(userTrip);

        // Mock Saving behavior
        when(tripRepository.save(any(Trip.class))).thenReturn(userTrip);
        when(tripPointMapper.toEntities(anyList(), any(), any())).thenReturn(new ArrayList<>());

        // Mock Weather Service success
        when(openMeteoService.getWeatherData(any(), any(), any(), any())).thenReturn(new MeteorologicalData());

        Trip createdTrip = tripService.recordTripManually(request);

        assertNotNull(createdTrip);
        assertEquals(userTrip.getId(), createdTrip.getId());

        // Verify flow
        verify(mapboxService).geocodeAddresses(addresses);
        verify(mapboxService).calculateCyclingRoute(anyList());
        verify(tripPointRepository).saveAll(anyList()); // Points saved
        verify(openMeteoService).getWeatherData(any(), any(), any(), any()); // Weather fetched
        verify(tripRepository, times(2)).save(any(Trip.class));
    }

    @Test
    @DisplayName("Should record trip manually even if weather service fails")
    void recordTripManually_WeatherFailure_Success() {
        // Prepare request
        List<String> addresses = List.of("Milan", "Monza");
        TripManualRecordRequest request = new TripManualRecordRequest(
                addresses, "Commute", OffsetDateTime.now(), OffsetDateTime.now().plusHours(1), null
        );

        // Mocks for basic flow
        when(userAuthService.getAuthenticatedUser()).thenReturn(currentUser);
        when(mapboxService.geocodeAddresses(anyList())).thenReturn(List.of(
                new GeocodeResult("A", new Coordinate(0.0, 0.0)),
                new GeocodeResult("B", new Coordinate(1.0, 1.0))
        ));
        when(mapboxService.calculateCyclingRoute(any())).thenReturn(new CyclingRouteResult(new ArrayList<>(), 1000.0));
        when(tripMapper.toEntity(any(), any(), any(), any(), anyInt(), any(), any(), any())).thenReturn(userTrip);
        when(tripRepository.save(any(Trip.class))).thenReturn(userTrip);
        when(tripPointMapper.toEntities(anyList(), any(), any())).thenReturn(new ArrayList<>());

        // Mock Weather Service throwing exception
        when(openMeteoService.getWeatherData(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Weather API down"));

        Trip createdTrip = tripService.recordTripManually(request);

        // Should still return the trip, just without weather data validation
        assertNotNull(createdTrip);
        verify(tripPointRepository).saveAll(anyList());
        // Verify we tried to fetch weather
        verify(openMeteoService).getWeatherData(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should retrieve trip by ID successfully when user is owner")
    void getTripById_Success() {
        when(userAuthService.getAuthenticatedUser()).thenReturn(currentUser);
        when(tripRepository.findById(100L)).thenReturn(Optional.of(userTrip));

        Trip result = tripService.getTripById(100L);

        assertEquals(userTrip, result);
        verify(tripRepository).findById(100L);
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when retrieving trip of another user")
    void getTripById_NotOwner_ThrowsException() {
        User otherUser = User.builder().id(999L).build();
        Trip otherTrip = Trip.builder().id(200L).recordedBy(otherUser).build();

        when(userAuthService.getAuthenticatedUser()).thenReturn(currentUser);
        when(tripRepository.findById(200L)).thenReturn(Optional.of(otherTrip));

        assertThrows(AccessDeniedException.class, () -> tripService.getTripById(200L));
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when trip does not exist")
    void getTripById_NotFound_ThrowsException() {
        when(userAuthService.getAuthenticatedUser()).thenReturn(currentUser);
        when(tripRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> tripService.getTripById(99L));
    }

    @Test
    @DisplayName("Should delete trip successfully when user is owner")
    void deleteTrip_Success() {
        when(userAuthService.getAuthenticatedUser()).thenReturn(currentUser);
        when(tripRepository.findById(100L)).thenReturn(Optional.of(userTrip));

        tripService.deleteTrip(100L);

        verify(tripRepository).delete(userTrip);
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when deleting another user's trip")
    void deleteTrip_NotOwner_ThrowsException() {
        User otherUser = User.builder().id(999L).build();
        Trip otherTrip = Trip.builder().id(200L).recordedBy(otherUser).build();

        when(userAuthService.getAuthenticatedUser()).thenReturn(currentUser);
        when(tripRepository.findById(200L)).thenReturn(Optional.of(otherTrip));

        assertThrows(AccessDeniedException.class, () -> tripService.deleteTrip(200L));

        verify(tripRepository, never()).delete(any(Trip.class));
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when deleting non-existent trip")
    void deleteTrip_NotFound_ThrowsException() {
        when(userAuthService.getAuthenticatedUser()).thenReturn(currentUser);
        when(tripRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> tripService.deleteTrip(99L));
        verify(tripRepository, never()).delete(any(Trip.class));
    }

    @Test
    @DisplayName("Should retrieve paginated user trips with enrichment")
    void getUserTrips_Success() {
        Page<Trip> page = new PageImpl<>(List.of(userTrip));

        when(userAuthService.getAuthenticatedUser()).thenReturn(currentUser);
        when(tripRepository.findPageByRecordedByIdWithWeather(eq(currentUser.getId()), any(Pageable.class)))
                .thenReturn(page);

        // Mock enrichment
        when(tripPointRepository.findAllByTripIdInOrderByTripIdAscSequentialPositionAsc(anyList()))
                .thenReturn(new ArrayList<>());

        Page<Trip> result = tripService.getUserTrips(0, 5, "startTime", "DESC");

        assertEquals(1, result.getTotalElements());
        verify(tripPointRepository).findAllByTripIdInOrderByTripIdAscSequentialPositionAsc(anyList());
    }

    @Test
    @DisplayName("Should search trips with filters and return enriched results")
    void searchTrips_Success() {
        // Prepare Search Request matching the record definition
        TripSearchRequest searchRequest = new TripSearchRequest(
                "Milan", "Monza", null, null
        );

        Page<Trip> page = new PageImpl<>(List.of(userTrip));

        when(userAuthService.getAuthenticatedUser()).thenReturn(currentUser);

        // Mock Repository Search
        when(tripRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        // Mock Enrichment
        when(tripPointRepository.findAllByTripIdInOrderByTripIdAscSequentialPositionAsc(anyList()))
                .thenReturn(new ArrayList<>());

        Page<Trip> result = tripService.searchTrips(searchRequest, 0, 10, "startTime", "DESC");

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(userTrip, result.getContent().get(0));

        verify(tripRepository).findAll(any(Specification.class), any(Pageable.class));
        verify(tripPointRepository).findAllByTripIdInOrderByTripIdAscSequentialPositionAsc(anyList());
    }
}