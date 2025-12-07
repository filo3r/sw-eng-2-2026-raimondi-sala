package it.polimi.se.bbp.service.openmeteo;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import it.polimi.se.bbp.config.openmeteo.OpenMeteoConfig;
import it.polimi.se.bbp.dto.openmeteo.OpenMeteoResponse;
import it.polimi.se.bbp.dto.result.MeteorologicalDataResult;
import it.polimi.se.bbp.entity.MeteorologicalData;
import it.polimi.se.bbp.entity.Trip;
import it.polimi.se.bbp.enums.openmeteo.WeatherCondition;
import it.polimi.se.bbp.exception.openmeteo.OpenMeteoApiException;
import it.polimi.se.bbp.exception.openmeteo.OpenMeteoRateLimitException;
import it.polimi.se.bbp.exception.openmeteo.OpenMeteoTimeoutException;
import it.polimi.se.bbp.exception.openmeteo.WeatherDataNotAvailableException;
import it.polimi.se.bbp.mapper.entity.MeteorologicalDataMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Service for interacting with Open-Meteo Forecast API.
 * Retrieves historical and forecast weather data for specific locations and times.
 * All timestamp handling in UTC for consistency across timezones.
 * Optimized with automatic JSON parsing/validation, caching (2-decimal coordinates,
 * hour-rounded timestamps), rate limiting (10 req/sec), and self-injection for cache.
 */
@Service
@Slf4j
public class OpenMeteoService {

    /**
     * Maximum weather requests per second.
     */
    private static final int OPENMETEO_REQUESTS_PER_SECOND = 10;

    /**
     * Rate limiter refresh period in seconds.
     */
    private static final int RATE_LIMIT_REFRESH_PERIOD_SECONDS = 1;

    /**
     * Maximum wait time for rate limiter permission in seconds.
     */
    private static final int RATE_LIMITER_TIMEOUT_SECONDS = 5;

    /**
     * Maximum days in past for weather data retrieval.
     * Open-Meteo Forecast API provides historical data for approximately last 3 months.
     */
    private static final int MAX_DAYS_IN_PAST = 90;

    /**
     * Formatter for parsing ISO 8601 timestamps from Open-Meteo API.
     * Format: "2025-11-11T14:00" (without Z suffix when timezone=UTC specified).
     */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Formatter for date parameters in API requests.
     * Format: "2025-11-11"
     */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    /**
     * Open-Meteo API configuration (base URL, endpoints, timeout, timezone).
     */
    private final OpenMeteoConfig openMeteoConfig;

    /**
     * REST client configured for Open-Meteo API calls.
     * Handles JSON deserialization into DTOs automatically.
     */
    private final RestClient restClient;

    /**
     * Mapper for converting weather data parameters to MeteorologicalData entities.
     */
    private final MeteorologicalDataMapper meteorologicalDataMapper;

    /**
     * Rate limiter for Open-Meteo API (10 req/sec, based on 600 calls/min limit).
     */
    private final RateLimiter rateLimiter;

    /**
     * Self-reference to Spring proxy.
     * Enables cache interception on internal method calls.
     * Lazy annotation prevents circular dependency.
     */
    private final OpenMeteoService self;

    /**
     * Initializes rate limiter based on Open-Meteo API limits.
     * @param openMeteoConfig Open-Meteo API configuration
     * @param restClient REST client configured for Open-Meteo API
     * @param meteorologicalDataMapper mapper for creating MeteorologicalData entities
     * @param self self-reference for cache interception
     */
    public OpenMeteoService(OpenMeteoConfig openMeteoConfig, @Qualifier("openMeteoRestClient") RestClient restClient, MeteorologicalDataMapper meteorologicalDataMapper, @Lazy OpenMeteoService self) {
        this.openMeteoConfig = openMeteoConfig;
        this.restClient = restClient;
        this.meteorologicalDataMapper = meteorologicalDataMapper;
        this.rateLimiter = RateLimiter.of("openMeteo", RateLimiterConfig.custom()
                .limitForPeriod(OPENMETEO_REQUESTS_PER_SECOND)
                .limitRefreshPeriod(Duration.ofSeconds(RATE_LIMIT_REFRESH_PERIOD_SECONDS))
                .timeoutDuration(Duration.ofSeconds(RATE_LIMITER_TIMEOUT_SECONDS))
                .build());
        this.self = self;
    }

    /**
     * Retrieves weather data and creates MeteorologicalData entity.
     * Public method for services to call. Fetches via fetchWeatherData then converts to entity.
     * @param latitude latitude coordinate
     * @param longitude longitude coordinate
     * @param startTime trip start time
     * @param trip trip entity to associate with meteorological data
     * @return MeteorologicalData entity ready to be persisted
     */
    public MeteorologicalData getWeatherData(Double latitude, Double longitude, OffsetDateTime startTime, Trip trip) {
        // Get cached weather data (DTO)
        MeteorologicalDataResult result = self.fetchWeatherData(latitude, longitude, startTime);
        // Convert to entity with the specific trip
        return meteorologicalDataMapper.toEntity(
                trip,
                result.weatherCondition(),
                result.temperature(),
                result.humidity(),
                result.windSpeed()
        );
    }

    /**
     * Retrieves weather data for specific location and time.
     * Results cached, rate limited to 10 req/sec, all timestamp operations in UTC.
     * Cache strategy: coordinates rounded to 2 decimals (~1 km), time rounded to nearest hour in UTC.
     * @param latitude latitude coordinate (-90 to 90, validated by caller)
     * @param longitude longitude coordinate (-180 to 180, validated by caller)
     * @param startTime trip start time (any timezone, converted to UTC)
     * @return MeteorologicalDataResult with weather conditions closest to start time
     * @throws IllegalArgumentException if parameters are null
     * @throws WeatherDataNotAvailableException if start time older than 90 days or no data found
     * @throws OpenMeteoRateLimitException if rate limit exceeded
     * @throws OpenMeteoTimeoutException if request times out
     * @throws OpenMeteoApiException if Open-Meteo returns error or malformed response
     */
    @Cacheable(value = "weatherData", key = "T(java.lang.String).format('%.2f_%.2f_%s', #latitude, #longitude, @openMeteoService.roundToNearestHour(#startTime))", sync = true)
    public MeteorologicalDataResult fetchWeatherData(Double latitude, Double longitude, OffsetDateTime startTime) {
        // Defensive check
        if (latitude == null || longitude == null || startTime == null)
            throw new IllegalArgumentException("Parameters cannot be null: latitude, longitude, startTime, and trip are required");
        // Acquire rate limiter permit
        try {
            RateLimiter.waitForPermission(rateLimiter);
        } catch (RequestNotPermitted e) {
            OpenMeteoRateLimitException exception = new OpenMeteoRateLimitException("Weather service is temporarily unavailable due to high traffic. Please try again later.", e);
            log.warn("Open-Meteo rate limit exceeded while acquiring permit", exception);
            throw exception;
        }
        // Execute Open-Meteo API request and handle infrastructure exceptions
        try {
            // Convert startTime to UTC for consistent processing
            OffsetDateTime utcStartTime = startTime.withOffsetSameInstant(ZoneOffset.UTC);
            // Validate that the start time is within the supported historical range
            validateStartTimeRange(utcStartTime);
            // Build the API request URL with coordinates and date parameters
            String url = buildWeatherApiUrl(latitude, longitude, utcStartTime);
            // Execute HTTP GET request and retrieve response body directly as OpenMeteoResponse DTO
            OpenMeteoResponse response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(OpenMeteoResponse.class);
            // Check for null response
            if (response == null) {
                OpenMeteoApiException exception = new OpenMeteoApiException("Received null response from Open-Meteo API");
                log.error("Open-Meteo API returned null response", exception);
                throw exception;
            }
            // Find the closest hour and extract weather data
            int closestIndex = findClosestHourIndex(response.hourlyData().time(), utcStartTime);
            // Extract weather values for the closest hour
            OpenMeteoResponse.HourlyData hourlyData = response.hourlyData();
            Double temperature = hourlyData.temperatureValues().get(closestIndex);
            Integer humidity = hourlyData.humidityValues().get(closestIndex);
            Double windSpeed = hourlyData.windSpeedValues().get(closestIndex);
            Integer weatherCode = hourlyData.weatherCodeValues().get(closestIndex);
            WeatherCondition weatherCondition = WeatherCondition.fromWeatherCode(weatherCode);
            // Return result DTO
            return new MeteorologicalDataResult(weatherCondition, temperature, humidity, windSpeed);
        } catch (WeatherDataNotAvailableException | OpenMeteoApiException e) {
            // Domain exceptions - re-throw without wrapping
            throw e;
        } catch (ResourceAccessException e) {
            // Timeout or network connection failure
            OpenMeteoTimeoutException exception = new OpenMeteoTimeoutException("Weather request timed out. Please try again.", e);
            log.warn("Open-Meteo API request timed out", exception);
            throw exception;
        } catch (HttpClientErrorException e) {
            // HTTP 4xx errors - special handling for rate limit (429)
            if (e.getStatusCode().value() == 429) {
                OpenMeteoRateLimitException exception = new OpenMeteoRateLimitException("Open-Meteo API rate limit exceeded. Please try again later.", e);
                log.warn("Open-Meteo API rate limit exceeded (HTTP 429)", exception);
                throw exception;
            }
            OpenMeteoApiException exception = new OpenMeteoApiException(String.format("Open-Meteo API client error: %s", e.getStatusCode()), e);
            log.error("Open-Meteo API client error (HTTP {})", e.getStatusCode().value(), exception);
            throw exception;
        } catch (HttpServerErrorException e) {
            // HTTP 5xx errors - Open-Meteo server issues
            OpenMeteoApiException exception = new OpenMeteoApiException(String.format("Open-Meteo API server error: %s", e.getStatusCode()), e);
            log.error("Open-Meteo API server error (HTTP {})", e.getStatusCode().value(), exception);
            throw exception;
        } catch (RestClientException e) {
            // Other REST errors (JSON parsing, encoding, etc.)
            OpenMeteoApiException exception = new OpenMeteoApiException("Open-Meteo API request failed", e);
            log.error("Open-Meteo API request failed", exception);
            throw exception;
        } catch (Exception e) {
            // Safety net for unexpected errors
            OpenMeteoApiException exception = new OpenMeteoApiException("Unexpected error during weather data retrieval", e);
            log.error("Unexpected error during Open-Meteo weather data retrieval", exception);
            throw exception;
        }
    }

    /**
     * Rounds OffsetDateTime to nearest hour in UTC for cache key generation.
     * Ensures cache keys aligned with findClosestHourIndex logic.
     * All rounding in UTC for consistency across timezones.
     * Rounding: 00-29 minutes rounds down, 30-59 minutes rounds up.
     * @param dateTime date and time to round (any timezone)
     * @return OffsetDateTime rounded to nearest hour in UTC
     */
    public OffsetDateTime roundToNearestHour(OffsetDateTime dateTime) {
        // Convert to UTC first to ensure consistent rounding across timezones
        OffsetDateTime utc = dateTime.withOffsetSameInstant(ZoneOffset.UTC);
        int minutes = utc.getMinute();
        // Round up if minutes >= 30, otherwise round down
        if (minutes >= 30) {
            return utc.truncatedTo(ChronoUnit.HOURS).plusHours(1);
        }
        return utc.truncatedTo(ChronoUnit.HOURS);
    }

    /**
     * Builds complete Open-Meteo API URL with all required parameters in UTC.
     * Converts dateTime to UTC before extracting date for correct calendar day.
     * @param latitude latitude coordinate
     * @param longitude longitude coordinate
     * @param dateTime date and time for weather data (should already be UTC from caller)
     * @return complete API URL
     */
    private String buildWeatherApiUrl(Double latitude, Double longitude, OffsetDateTime dateTime) {
        // Ensure we're in UTC before extracting date (should already be UTC from caller, but double-check)
        OffsetDateTime utc = dateTime.withOffsetSameInstant(ZoneOffset.UTC);
        String date = utc.format(DATE_FORMATTER);
        // Build URL with all required query parameters
        return openMeteoConfig.getForecastEndpoint() +
                "?latitude=" + latitude +
                "&longitude=" + longitude +
                "&hourly=temperature_2m,relative_humidity_2m,wind_speed_10m,weather_code" +
                "&start_date=" + date +
                "&end_date=" + date +
                "&timezone=" + openMeteoConfig.getTimezone();
    }

    /**
     * Finds index of hour in time array closest to given start time.
     * Uses absolute time difference, all comparisons in UTC.
     * @param timeArray ISO 8601 timestamps from Open-Meteo (without Z suffix, in UTC)
     * @param startTime trip start time to match (should already be UTC from caller)
     * @return index of closest hour in array
     * @throws WeatherDataNotAvailableException if no valid timestamp found
     * @throws OpenMeteoApiException if any timestamp has invalid format
     */
    private int findClosestHourIndex(List<String> timeArray, OffsetDateTime startTime) {
        // Ensure startTime is in UTC for comparison (should already be from caller, but double-check)
        OffsetDateTime utcStartTime = startTime.withOffsetSameInstant(ZoneOffset.UTC);
        int closestIndex = -1;
        long minDifference = Long.MAX_VALUE;
        // Iterate through all timestamps to find the closest match
        for (int i = 0; i < timeArray.size(); i++) {
            try {
                // Open-Meteo returns timestamps like "2025-11-13T14:00" (without Z) when timezone=UTC
                // Parse as LocalDateTime first, then convert to UTC OffsetDateTime
                LocalDateTime localDateTime = LocalDateTime.parse(timeArray.get(i), TIME_FORMATTER);
                OffsetDateTime hourTime = localDateTime.atOffset(ZoneOffset.UTC);
                // Calculate time difference in minutes (UTC comparison)
                long differenceInMinutes = Math.abs(ChronoUnit.MINUTES.between(utcStartTime, hourTime));
                // Update the closest index if this hour is closer
                if (differenceInMinutes < minDifference) {
                    minDifference = differenceInMinutes;
                    closestIndex = i;
                }
            } catch (DateTimeException e) {
                // Malformed timestamp = API error
                OpenMeteoApiException exception = new OpenMeteoApiException(String.format("Invalid timestamp format in API response: %s", timeArray.get(i)), e);
                log.error("Invalid timestamp format received from Open-Meteo API", exception);
                throw exception;
            }
        }
        // Throw exception if no valid timestamp was found
        if (closestIndex == -1) {
            throw new WeatherDataNotAvailableException("No valid weather data found for the trip start time");
        }
        return closestIndex;
    }

    /**
     * Validates start time is within supported historical data range.
     * Open-Meteo provides historical data for approximately last 90 days.
     * All date calculations in UTC.
     * @param startTime trip start time to validate (should already be UTC from caller)
     * @throws WeatherDataNotAvailableException if start time older than 90 days from now
     */
    private void validateStartTimeRange(OffsetDateTime startTime) {
        // Use UTC for current time to ensure consistent validation across timezones
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        // Calculate the number of days between start time and now
        long daysDifference = ChronoUnit.DAYS.between(startTime, now);
        // Throw exception if start time is too far in the past
        if (daysDifference > MAX_DAYS_IN_PAST) {
            throw new WeatherDataNotAvailableException(
                    String.format("Weather data is not available for trips older than %d days. Trip is %d days old.",
                            MAX_DAYS_IN_PAST, daysDifference)
            );
        }
    }

}