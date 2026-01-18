package it.polimi.se.bbp.service.openmeteo;

import it.polimi.se.bbp.config.openmeteo.OpenMeteoConfig;
import it.polimi.se.bbp.dto.openmeteo.OpenMeteoResponse;
import it.polimi.se.bbp.dto.result.MeteorologicalDataResult;
import it.polimi.se.bbp.entity.MeteorologicalData;
import it.polimi.se.bbp.entity.Trip;
import it.polimi.se.bbp.enums.openmeteo.WeatherCondition;
import it.polimi.se.bbp.exception.openmeteo.*;
import it.polimi.se.bbp.mapper.entity.MeteorologicalDataMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Open-Meteo Service Tests")
class OpenMeteoServiceTest {

    @Mock
    private OpenMeteoConfig openMeteoConfig;

    @Mock
    private RestClient restClient;

    @Mock
    private MeteorologicalDataMapper meteorologicalDataMapper;

    @Mock
    private OpenMeteoService self;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock
    private RestClient.RequestHeadersSpec requestHeadersSpec;
    @Mock
    private RestClient.ResponseSpec responseSpec;

    @InjectMocks
    private OpenMeteoService openMeteoService;

    private Trip trip;
    private OffsetDateTime startTime;

    @BeforeEach
    void setUp() {
        trip = Trip.builder().id(1L).build();
        startTime = OffsetDateTime.now(ZoneOffset.UTC).minusDays(2);
    }

    @Test
    @DisplayName("Should retrieve weather data entity successfully")
    void getWeatherData_Success() {
        // We simulate a scenario where the weather data result is successfully retrieved via fetchWeatherData
        Double lat = 45.0;
        Double lon = 9.0;
        MeteorologicalDataResult resultDTO = new MeteorologicalDataResult(
                WeatherCondition.CLEAR_SKY, 25.0, 50, 10.0
        );
        MeteorologicalData expectedEntity = new MeteorologicalData();

        when(self.fetchWeatherData(lat, lon, startTime)).thenReturn(resultDTO);

        when(meteorologicalDataMapper.toEntity(
                trip,
                resultDTO.weatherCondition(),
                resultDTO.temperature(),
                resultDTO.humidity(),
                resultDTO.windSpeed()
        )).thenReturn(expectedEntity);

        MeteorologicalData result = openMeteoService.getWeatherData(lat, lon, startTime, trip);

        assertNotNull(result);
        assertEquals(expectedEntity, result);
        verify(self).fetchWeatherData(lat, lon, startTime);
        verify(meteorologicalDataMapper).toEntity(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should fetch weather data successfully from API")
    void fetchWeatherData_Success() {
        // We simulate the RestClient chain to return a valid OpenMeteoResponse
        Double lat = 45.0;
        Double lon = 9.0;

        String timeStr = startTime.withMinute(0).withSecond(0).withNano(0).toString().replace("Z", "");

        OpenMeteoResponse.HourlyData hourlyData = new OpenMeteoResponse.HourlyData(
                List.of(timeStr),
                List.of(20.5),
                List.of(60),
                List.of(15.0),
                List.of(0)
        );
        OpenMeteoResponse mockResponse = new OpenMeteoResponse(lat, lon, "UTC", hourlyData);

        when(openMeteoConfig.getForecastEndpoint()).thenReturn("/v1/forecast");
        when(openMeteoConfig.getTimezone()).thenReturn("UTC");

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(OpenMeteoResponse.class)).thenReturn(mockResponse);

        MeteorologicalDataResult result = openMeteoService.fetchWeatherData(lat, lon, startTime);

        assertNotNull(result);
        assertEquals(WeatherCondition.CLEAR_SKY, result.weatherCondition());
        assertEquals(20.5, result.temperature());
        assertEquals(60, result.humidity());
        assertEquals(15.0, result.windSpeed());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when parameters are null")
    void fetchWeatherData_NullParams_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
                openMeteoService.fetchWeatherData(null, 9.0, startTime)
        );
        assertThrows(IllegalArgumentException.class, () ->
                openMeteoService.fetchWeatherData(45.0, null, startTime)
        );
        assertThrows(IllegalArgumentException.class, () ->
                openMeteoService.fetchWeatherData(45.0, 9.0, null)
        );
    }

    @Test
    @DisplayName("Should throw WeatherDataNotAvailableException for old dates")
    void fetchWeatherData_OldDate_ThrowsException() {
        // If the date is too far in the past, the service should reject the request
        OffsetDateTime oldDate = OffsetDateTime.now(ZoneOffset.UTC).minusDays(91);
        assertThrows(WeatherDataNotAvailableException.class, () ->
                openMeteoService.fetchWeatherData(45.0, 9.0, oldDate)
        );
    }

    @Test
    @DisplayName("Should throw OpenMeteoApiException when response is null")
    void fetchWeatherData_NullResponse_ThrowsException() {
        // If the external API returns a success code but an empty body, we treat it as an API error
        when(openMeteoConfig.getForecastEndpoint()).thenReturn("/v1/forecast");
        when(openMeteoConfig.getTimezone()).thenReturn("UTC");

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(OpenMeteoResponse.class)).thenReturn(null);

        assertThrows(OpenMeteoApiException.class, () ->
                openMeteoService.fetchWeatherData(45.0, 9.0, startTime)
        );
    }

    @Test
    @DisplayName("Should throw OpenMeteoTimeoutException on network timeout")
    void fetchWeatherData_Timeout_ThrowsException() {
        when(openMeteoConfig.getForecastEndpoint()).thenReturn("/v1/forecast");
        when(openMeteoConfig.getTimezone()).thenReturn("UTC");

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenThrow(new ResourceAccessException("Timeout"));

        assertThrows(OpenMeteoTimeoutException.class, () ->
                openMeteoService.fetchWeatherData(45.0, 9.0, startTime)
        );
    }

    @Test
    @DisplayName("Should throw OpenMeteoRateLimitException on HTTP 429")
    void fetchWeatherData_RateLimit_ThrowsException() {
        // If the API returns a 429 status, we must throw the specific rate limit exception
        when(openMeteoConfig.getForecastEndpoint()).thenReturn("/v1/forecast");
        when(openMeteoConfig.getTimezone()).thenReturn("UTC");

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenThrow(
                new HttpClientErrorException(HttpStatusCode.valueOf(429))
        );

        assertThrows(OpenMeteoRateLimitException.class, () ->
                openMeteoService.fetchWeatherData(45.0, 9.0, startTime)
        );
    }

    @Test
    @DisplayName("Should throw OpenMeteoApiException on HTTP 500")
    void fetchWeatherData_ServerError_ThrowsException() {
        when(openMeteoConfig.getForecastEndpoint()).thenReturn("/v1/forecast");
        when(openMeteoConfig.getTimezone()).thenReturn("UTC");

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenThrow(
                new HttpServerErrorException(HttpStatusCode.valueOf(500))
        );

        assertThrows(OpenMeteoApiException.class, () ->
                openMeteoService.fetchWeatherData(45.0, 9.0, startTime)
        );
    }

    @Test
    @DisplayName("Should round time to nearest hour correctly")
    void roundToNearestHour_Logic() {
        OffsetDateTime time1 = OffsetDateTime.parse("2023-10-10T10:15:00Z");
        assertEquals(10, openMeteoService.roundToNearestHour(time1).getHour());

        OffsetDateTime time2 = OffsetDateTime.parse("2023-10-10T10:45:00Z");
        assertEquals(11, openMeteoService.roundToNearestHour(time2).getHour());

        OffsetDateTime time3 = OffsetDateTime.parse("2023-10-10T10:30:00Z");
        assertEquals(11, openMeteoService.roundToNearestHour(time3).getHour());
    }
}