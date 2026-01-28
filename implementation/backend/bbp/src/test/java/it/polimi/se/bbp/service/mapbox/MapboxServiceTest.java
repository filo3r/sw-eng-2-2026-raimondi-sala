package it.polimi.se.bbp.service.mapbox;

import it.polimi.se.bbp.config.mapbox.MapboxConfig;
import it.polimi.se.bbp.dto.mapbox.MapboxDirectionsResponse;
import it.polimi.se.bbp.dto.mapbox.MapboxGeocodeResponse;
import it.polimi.se.bbp.dto.result.CyclingRouteResult;
import it.polimi.se.bbp.dto.result.GeocodeResult;
import it.polimi.se.bbp.exception.mapbox.*;
import it.polimi.se.bbp.geo.Coordinate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Mapbox Service Tests")
class MapboxServiceTest {

    @Mock
    private RestClient mapboxRestClient;

    @Mock
    private MapboxConfig mapboxConfig;

    @Mock
    private MapboxService self;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock
    private RestClient.RequestHeadersSpec requestHeadersSpec;
    @Mock
    private RestClient.ResponseSpec responseSpec;

    private MapboxService mapboxService;

    @BeforeEach
    void setUp() {
        // Initialize the service with the self-reference and inject necessary endpoint URLs via reflection
        mapboxService = new MapboxService(mapboxRestClient, mapboxConfig, self);

        ReflectionTestUtils.setField(mapboxService, "searchBoxForwardEndpoint", "https://api.mapbox.com/search/v1");
        ReflectionTestUtils.setField(mapboxService, "directionsEndpoint", "https://api.mapbox.com/directions/v5/mapbox/cycling");

        lenient().when(mapboxConfig.getApiKey()).thenReturn("pk.test-token");
    }

    @Test
    @DisplayName("Should geocode address successfully")
    void geocodeAddress_Success() {
        // Simulate a successful API response with geometry and properties, ensuring the service maps it correctly
        String address = "Via Roma, Milano";
        Coordinate coord = new Coordinate(45.0, 9.0);

        MapboxGeocodeResponse.Geometry geometry = new MapboxGeocodeResponse.Geometry(List.of(9.0, 45.0));
        MapboxGeocodeResponse.Properties props = new MapboxGeocodeResponse.Properties(address, "Via Roma", "Milano");
        MapboxGeocodeResponse.Feature feature = new MapboxGeocodeResponse.Feature(geometry, props);
        MapboxGeocodeResponse response = new MapboxGeocodeResponse(List.of(feature));

        mockRestClientCall(response, MapboxGeocodeResponse.class);

        GeocodeResult result = mapboxService.geocodeAddress(address);

        assertNotNull(result);
        assertEquals(address, result.address());
        assertEquals(coord.getLatitude(), result.coordinate().getLatitude());
        assertEquals(coord.getLongitude(), result.coordinate().getLongitude());
    }

    @Test
    @DisplayName("Should throw InvalidAddressException when no features found")
    void geocodeAddress_NotFound_ThrowsException() {
        // If the API returns a valid response but contains no features, the service should throw an InvalidAddressException
        MapboxGeocodeResponse response = new MapboxGeocodeResponse(Collections.emptyList());
        mockRestClientCall(response, MapboxGeocodeResponse.class);

        assertThrows(InvalidAddressException.class, () -> mapboxService.geocodeAddress("Unknown Place"));
    }

    @Test
    @DisplayName("Should throw MapboxRateLimitException on HTTP 429")
    void geocodeAddress_RateLimit_ThrowsException() {
        // Simulate an API rate limit (HTTP 429) to ensure the service translates it into a specific MapboxRateLimitException
        when(mapboxRestClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenThrow(new HttpClientErrorException(HttpStatusCode.valueOf(429)));

        assertThrows(MapboxRateLimitException.class, () -> mapboxService.geocodeAddress("Any Address"));
    }

    @Test
    @DisplayName("Should throw MapboxTimeoutException on timeout")
    void geocodeAddress_Timeout_ThrowsException() {
        // Simulate a network timeout to verify that a MapboxTimeoutException is thrown
        when(mapboxRestClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenThrow(new ResourceAccessException("Timeout"));

        assertThrows(MapboxTimeoutException.class, () -> mapboxService.geocodeAddress("Any Address"));
    }

    @Test
    @DisplayName("Should geocode multiple addresses in parallel batches")
    void geocodeAddresses_Success() {
        // Mock internal self-invocations to simulate parallel processing of multiple addresses and verify result aggregation
        List<String> addresses = List.of("Addr 1", "Addr 2", "Addr 3");
        GeocodeResult res1 = new GeocodeResult("Addr 1", new Coordinate(1.0, 1.0));
        GeocodeResult res2 = new GeocodeResult("Addr 2", new Coordinate(2.0, 2.0));
        GeocodeResult res3 = new GeocodeResult("Addr 3", new Coordinate(3.0, 3.0));

        when(self.geocodeAddress("Addr 1")).thenReturn(res1);
        when(self.geocodeAddress("Addr 2")).thenReturn(res2);
        when(self.geocodeAddress("Addr 3")).thenReturn(res3);

        List<GeocodeResult> results = mapboxService.geocodeAddresses(addresses);

        assertEquals(3, results.size());
        assertEquals(res1, results.get(0));
        assertEquals(res2, results.get(1));
        assertEquals(res3, results.get(2));
        verify(self, times(3)).geocodeAddress(anyString());
    }

    @Test
    @DisplayName("Should unwrap and propagate exceptions during batch geocoding")
    void geocodeAddresses_ExceptionPropagation() {
        // Ensure that an exception thrown during the processing of a single address in the batch immediately propagates up
        List<String> addresses = List.of("Valid", "Invalid");

        when(self.geocodeAddress("Valid")).thenReturn(new GeocodeResult("Valid", new Coordinate(0.0, 0.0)));
        when(self.geocodeAddress("Invalid")).thenThrow(new InvalidAddressException("Invalid"));

        assertThrows(InvalidAddressException.class, () -> mapboxService.geocodeAddresses(addresses));
    }

    @Test
    @DisplayName("Should calculate single route successfully")
    void calculateSingleRoute_Success() {
        // Simulate a valid Mapbox Directions API response and verify that the distance and geometry are correctly extracted
        List<Coordinate> waypoints = List.of(new Coordinate(45.0, 9.0), new Coordinate(45.1, 9.1));

        MapboxDirectionsResponse.Geometry geometry = new MapboxDirectionsResponse.Geometry(
                List.of(List.of(9.0, 45.0), List.of(9.1, 45.1))
        );
        MapboxDirectionsResponse.Route route = new MapboxDirectionsResponse.Route(geometry, 1500.0);
        MapboxDirectionsResponse response = new MapboxDirectionsResponse("Ok", List.of(route));

        mockRestClientCall(response, MapboxDirectionsResponse.class);

        CyclingRouteResult result = mapboxService.calculateSingleRoute(waypoints);

        assertNotNull(result);
        assertEquals(1500.0, result.distanceInMeters());
        assertEquals(2, result.routeCoordinates().size());
    }

    @Test
    @DisplayName("Should throw InvalidRouteException when Mapbox returns NoRoute")
    void calculateSingleRoute_NoRoute_ThrowsException() {
        // If the API responds with a "NoRoute" status code, the service must interpret this as an invalid route scenario
        List<Coordinate> waypoints = List.of(new Coordinate(45.0, 9.0), new Coordinate(45.1, 9.1));
        MapboxDirectionsResponse response = new MapboxDirectionsResponse("NoRoute", null);

        mockRestClientCall(response, MapboxDirectionsResponse.class);

        assertThrows(InvalidRouteException.class, () -> mapboxService.calculateSingleRoute(waypoints));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when fewer than 2 waypoints provided")
    void calculateCyclingRoute_TooFewWaypoints_ThrowsException() {
        // The service should reject route calculation requests that do not contain at least two coordinates
        List<Coordinate> singleWaypoint = List.of(new Coordinate(45.0, 9.0));

        assertThrows(IllegalArgumentException.class, () -> mapboxService.calculateCyclingRoute(singleWaypoint));
        assertThrows(IllegalArgumentException.class, () -> mapboxService.calculateCyclingRoute(null));
    }

    @Test
    @DisplayName("Should split large route into overlapping chunks and merge results")
    void calculateCyclingRoute_LargeRequest_SplitsIntoChunksAndMerges() {
        List<Coordinate> allWaypoints = new java.util.ArrayList<>();
        for (int i = 0; i < 30; i++) {
            allWaypoints.add(new Coordinate((double) i, (double) i));
        }
        // create 2 chunks and test calculateCyclingRoute method
        List<Coordinate> chunk1 = allWaypoints.subList(0, 25);
        List<Coordinate> chunk2 = allWaypoints.subList(24, 30);

        CyclingRouteResult result1 = new CyclingRouteResult(new java.util.ArrayList<>(chunk1), 1000.0);
        CyclingRouteResult result2 = new CyclingRouteResult(new java.util.ArrayList<>(chunk2), 500.0);

        when(self.calculateSingleRoute(argThat(list -> list != null && list.size() == 25)))
                .thenReturn(result1);

        when(self.calculateSingleRoute(argThat(list -> list != null && list.size() == 6)))
                .thenReturn(result2);

        CyclingRouteResult finalResult = mapboxService.calculateCyclingRoute(allWaypoints);

        assertNotNull(finalResult);
        assertEquals(1500.0, finalResult.distanceInMeters());
        assertEquals(30, finalResult.routeCoordinates().size());
        assertEquals(allWaypoints, finalResult.routeCoordinates());

        verify(self, times(2)).calculateSingleRoute(anyList());
    }

    @Test
    @DisplayName("Should delegate to calculateSingleRoute for small requests")
    void calculateCyclingRoute_SmallRequest_Delegates() {
        // Verify that standard route requests are delegated to the internal calculation logic without segmentation
        List<Coordinate> waypoints = List.of(new Coordinate(45.0, 9.0), new Coordinate(45.1, 9.1));
        CyclingRouteResult expected = new CyclingRouteResult(waypoints, 1000.0);

        when(self.calculateSingleRoute(waypoints)).thenReturn(expected);

        CyclingRouteResult result = mapboxService.calculateCyclingRoute(waypoints);

        assertEquals(expected, result);
        verify(self).calculateSingleRoute(waypoints);
    }

    // Helper to mock RestClient
    private <T> void mockRestClientCall(T responseBody, Class<T> responseClass) {
        when(mapboxRestClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(responseClass)).thenReturn(responseBody);
    }
}