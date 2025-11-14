package it.polimi.se.bbp.service.openmeteo;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import it.polimi.se.bbp.config.openmeteo.OpenMeteoConfig;
import it.polimi.se.bbp.dto.openmeteo.OpenMeteoResponse;
import it.polimi.se.bbp.entity.MeteorologicalData;
import it.polimi.se.bbp.entity.Trip;
import it.polimi.se.bbp.enums.openmeteo.WeatherCondition;
import it.polimi.se.bbp.mapper.entity.MeteorologicalDataMapper;
import it.polimi.se.bbp.mapper.openmeteo.OpenMeteoResponseMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Service for interacting with the Open-Meteo Forecast API.
 * Retrieves historical and forecast weather data for specific locations and times.
 * Handles all business logic including finding the closest hour to trip start time.
 * All timestamp handling is done in UTC for consistency across timezones.
 * Optimized with:
 * - Caching with 2-decimal coordinate precision (~1 km) and hour-rounded timestamps
 * - Aligned cache key generation with closest hour logic for consistency
 * - Rate limiting to respect Open-Meteo API limits (10 req/sec based on 600 calls/min limit)
 * - UTC timezone handling for multi-timezone support
 */
@Service
public class OpenMeteoService {

    /**
     * Configuration for Open-Meteo API (base URL, endpoints, timeout, timezone).
     */
    private final OpenMeteoConfig openMeteoConfig;

    /**
     * REST client configured specifically for Open-Meteo API calls.
     */
    private final RestClient restClient;

    /**
     * Mapper for parsing JSON responses from Open-Meteo API.
     */
    private final OpenMeteoResponseMapper responseMapper;

    /**
     * Mapper for converting weather data parameters to MeteorologicalData entities.
     */
    private final MeteorologicalDataMapper meteorologicalDataMapper;

    /**
     * Rate limiter for Open-Meteo API (10 requests per second, based on 600 calls/min limit).
     */
    private final RateLimiter rateLimiter;

    /**
     * Formatter for parsing ISO 8601 timestamps from Open-Meteo API.
     * Open-Meteo returns timestamps in format "2025-11-11T14:00" (without Z suffix)
     * when timezone=UTC is specified in the API request.
     * Format: "2025-11-11T14:00"
     */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Formatter for date parameters in API requests.
     * Format: "2025-11-11"
     */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    /**
     * Maximum number of days in the past for which weather data can be retrieved.
     * Open-Meteo Forecast API provides historical data for approximately the last 3 months.
     */
    private static final int MAX_DAYS_IN_PAST = 90;

    /**
     * Constructor for dependency injection.
     * Initializes rate limiter based on Open-Meteo API limits.
     * @param openMeteoConfig Configuration for Open-Meteo API
     * @param restClient REST client configured for Open-Meteo API calls
     * @param responseMapper Mapper for parsing JSON responses
     * @param meteorologicalDataMapper Mapper for creating MeteorologicalData entities
     */
    public OpenMeteoService(OpenMeteoConfig openMeteoConfig, @Qualifier("openMeteoRestClient") RestClient restClient, OpenMeteoResponseMapper responseMapper, MeteorologicalDataMapper meteorologicalDataMapper) {
        this.openMeteoConfig = openMeteoConfig;
        this.restClient = restClient;
        this.responseMapper = responseMapper;
        this.meteorologicalDataMapper = meteorologicalDataMapper;
        this.rateLimiter = RateLimiter.of("openMeteo", RateLimiterConfig.custom()
                .limitForPeriod(10)
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ofSeconds(5))
                .build());
    }

    /**
     * Retrieves weather data for a specific location and time.
     * Results are cached to avoid repeated API calls for the same location and time.
     * Rate limited to 10 requests per second.
     * All timestamp operations are performed in UTC for consistency across timezones.
     * Cache strategy:
     * - Coordinates are rounded to 2 decimals (~1 km precision) to group nearby locations
     * - Time is rounded to the nearest hour in UTC (14:00-14:29 → 14:00, 14:30-15:29 → 15:00)
     * - This ensures cache key alignment with the findClosestHourIndex logic
     * Process:
     * 1. Converts startTime to UTC if not already
     * 2. Validates that the start time is within the supported historical range (last 90 days)
     * 3. Constructs API URL with latitude, longitude, and date parameters (in UTC)
     * 4. Makes HTTP GET request to Open-Meteo Forecast API with timezone=UTC
     * 5. Parses JSON response into OpenMeteoResponse DTO
     * 6. Finds the hour closest to the provided start time (comparing in UTC)
     * 7. Extracts weather values for that specific hour
     * 8. Converts to MeteorologicalData entity
     * Exception handling:
     * - IllegalArgumentException: Invalid parameters (time out of range, no data) → 400 BAD_REQUEST
     * - IllegalStateException: Open-Meteo service unavailable or rate limit exceeded → 503 SERVICE_UNAVAILABLE
     * @param latitude the latitude coordinate of the location
     * @param longitude the longitude coordinate of the location
     * @param startTime the trip start time to find the closest weather data for (any timezone, will be converted to UTC)
     * @param trip the trip entity to associate with the meteorological data
     * @return MeteorologicalData entity with weather conditions closest to start time
     * @throws IllegalArgumentException if start time is older than 90 days, coordinates are invalid, or no weather data is available
     * @throws IllegalStateException if Open-Meteo API is unavailable or rate limit is exceeded
     */
    @Cacheable(value = "weatherData", key = "T(java.lang.String).format('%.2f_%.2f_%s', #latitude, #longitude, @openMeteoService.roundToNearestHour(#startTime))", sync = true)
    public MeteorologicalData getWeatherData(Double latitude, Double longitude, OffsetDateTime startTime, Trip trip) {
        try {
            // Acquire rate limiter permit (blocks if necessary, throws RequestNotPermitted after timeout)
            RateLimiter.waitForPermission(rateLimiter);
        } catch (RequestNotPermitted e) {
            throw new IllegalStateException("Weather service is temporarily unavailable due to high traffic. Please try again later.", e);
        }
        try {
            // Convert startTime to UTC if not already (for consistent processing)
            OffsetDateTime utcStartTime = startTime.withOffsetSameInstant(ZoneOffset.UTC);
            // Validate that the start time is within the supported historical range
            validateStartTimeRange(utcStartTime);
            // Build the API request URL with coordinates and date parameters
            String url = buildWeatherApiUrl(latitude, longitude, utcStartTime);
            // Execute HTTP GET request and retrieve response body as String
            String jsonResponse = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);
            // Parse JSON response into OpenMeteoResponse DTO
            OpenMeteoResponse response = responseMapper.fromJsonResponse(jsonResponse);
            // Find closest hour using UTC time comparison
            int closestIndex = findClosestHourIndex(response.getHourlyData().getTime(), utcStartTime);
            // Extract weather values for the closest hour
            OpenMeteoResponse.HourlyData hourlyData = response.getHourlyData();
            Double temperature = hourlyData.getTemperatureValues().get(closestIndex);
            Integer humidity = hourlyData.getHumidityValues().get(closestIndex);
            Double windSpeed = hourlyData.getWindSpeedValues().get(closestIndex);
            Integer weatherCode = hourlyData.getWeatherCodeValues().get(closestIndex);
            WeatherCondition weatherCondition = WeatherCondition.fromWeatherCode(weatherCode);
            // Convert to MeteorologicalData entity and return
            return meteorologicalDataMapper.toEntity(trip, weatherCondition, temperature, humidity, windSpeed);
        } catch (IllegalArgumentException e) {
            // Invalid parameters → 400
            throw e;
        } catch (IllegalStateException e) {
            // Service unavailable → 503
            throw e;
        } catch (Exception e) {
            // Any other error (network issues, etc.) - wrap as service unavailable → 503
            throw new IllegalStateException("Open-Meteo weather service is currently unavailable", e);
        }
    }

    /**
     * Rounds an OffsetDateTime to the nearest hour in UTC for cache key generation.
     * This ensures cache keys are aligned with the findClosestHourIndex logic.
     * All rounding is performed in UTC to ensure consistency across different timezones.
     * Rounding logic:
     * - 00-29 minutes: rounds down to current hour
     * - 30-59 minutes: rounds up to next hour
     * Examples (all converted to UTC first):
     * - 2025-11-13T14:00+01:00 → 2025-11-13T13:00Z → 2025-11-13T13:00Z
     * - 2025-11-13T14:10+01:00 → 2025-11-13T13:10Z → 2025-11-13T13:00Z
     * - 2025-11-13T14:29+01:00 → 2025-11-13T13:29Z → 2025-11-13T13:00Z
     * - 2025-11-13T14:30+01:00 → 2025-11-13T13:30Z → 2025-11-13T14:00Z
     * - 2025-11-13T14:45-05:00 → 2025-11-13T19:45Z → 2025-11-13T20:00Z
     * - 2025-11-13T14:59-05:00 → 2025-11-13T19:59Z → 2025-11-13T20:00Z
     * @param dateTime the date and time to round (any timezone)
     * @return OffsetDateTime rounded to the nearest hour in UTC
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
     * Builds the complete Open-Meteo API URL with all required parameters in UTC.
     * Converts the provided dateTime to UTC before extracting the date to ensure
     * the correct calendar day is requested for the weather data.
     * Example URL:
     * https://api.open-meteo.com/v1/forecast?latitude=45.3597&longitude=9.3250
     * &hourly=temperature_2m,relative_humidity_2m,wind_speed_10m,weather_code
     * &start_date=2025-11-13&end_date=2025-11-13&timezone=UTC
     * @param latitude the latitude coordinate
     * @param longitude the longitude coordinate
     * @param dateTime the date and time to get weather data for (should already be in UTC from caller)
     * @return the complete API URL as a String
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
                "&timezone=" + openMeteoConfig.getTimezone();  // Must be "UTC" in configuration
    }

    /**
     * Finds the index of the hour in the time array that is closest to the given start time.
     * Uses absolute time difference to find the nearest hour.
     * All comparisons are performed in UTC for consistency.
     * Algorithm:
     * 1. Convert startTime to UTC if not already
     * 2. Parse all timestamp strings from Open-Meteo (format: "2025-11-11T14:00" without Z)
     * 3. Convert parsed LocalDateTime to UTC OffsetDateTime
     * 4. Calculate absolute time difference between each hour and startTime (both in UTC)
     * 5. Return the index with minimum difference
     * Example:
     * - startTime: 2025-11-13T12:00:00-05:00 (New York) → converts to 2025-11-13T17:00:00Z
     * - time array from Open-Meteo: ["2025-11-13T16:00", "2025-11-13T17:00", "2025-11-13T18:00"]
     * - parsed as UTC: [16:00Z, 17:00Z, 18:00Z]
     * - differences: [60 min, 0 min, 60 min]
     * - returns: index 1 (17:00Z - exact match)
     * @param timeArray array of ISO 8601 timestamp strings from Open-Meteo (without Z suffix, in UTC)
     * @param startTime the trip start time to match against (should already be in UTC from caller)
     * @return the index of the closest hour in the array
     * @throws IllegalArgumentException if no valid timestamp is found
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
                // Update closest index if this hour is closer
                if (differenceInMinutes < minDifference) {
                    minDifference = differenceInMinutes;
                    closestIndex = i;
                }
            } catch (Exception e) {
                // Skip invalid timestamp and continue
                continue;
            }
        }
        // Throw exception if no valid timestamp was found
        if (closestIndex == -1) {
            throw new IllegalArgumentException("No valid weather data found for the trip start time");
        }
        return closestIndex;
    }

    /**
     * Validates that the start time is within the supported historical data range.
     * Open-Meteo Forecast API provides historical data for approximately the last 90 days.
     * If the start time is older than this, weather data cannot be retrieved.
     * All date calculations are performed in UTC.
     * @param startTime the trip start time to validate (should already be in UTC from caller)
     * @throws IllegalArgumentException if start time is older than 90 days from now
     */
    private void validateStartTimeRange(OffsetDateTime startTime) {
        // Use UTC for current time to ensure consistent validation across timezones
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        // Calculate the number of days between start time and now
        long daysDifference = ChronoUnit.DAYS.between(startTime, now);
        // Throw exception if start time is too far in the past
        if (daysDifference > MAX_DAYS_IN_PAST) {
            throw new IllegalArgumentException(
                    String.format("Weather data is not available for trips older than %d days. Trip is %d days old.",
                            MAX_DAYS_IN_PAST, daysDifference)
            );
        }
    }

}