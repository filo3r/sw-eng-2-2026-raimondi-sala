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
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Service for interacting with the Open-Meteo Forecast API.
 * Retrieves historical and forecast weather data for specific locations and times.
 * Handles all business logic including finding the closest hour to trip start time.
 * Optimized with:
 * - Caching with 2-decimal coordinate precision (~1 km) and hour-rounded timestamps
 * - Aligned cache key generation with closest hour logic for consistency
 * - Rate limiting to respect Open-Meteo API limits (10 req/sec based on 600 calls/min limit)
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
     *
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
     * Cache strategy:
     * - Coordinates are rounded to 2 decimals (~1 km precision) to group nearby locations
     * - Time is rounded to the nearest hour (14:00-14:29 → 14:00, 14:30-15:29 → 15:00)
     * - This ensures cache key alignment with the findClosestHourIndex logic
     * Process:
     * 1. Validates that the start time is within the supported historical range (last 90 days)
     * 2. Constructs API URL with latitude, longitude, and date parameters
     * 3. Makes HTTP GET request to Open-Meteo Forecast API
     * 4. Parses JSON response into OpenMeteoResponse DTO
     * 5. Finds the hour closest to the provided start time
     * 6. Extracts weather values for that specific hour
     * 7. Converts to MeteorologicalData entity
     * Exception handling:
     * - IllegalArgumentException: Invalid parameters (time out of range, no data) → 400 BAD_REQUEST
     * - IllegalStateException: Open-Meteo service unavailable or rate limit exceeded → 503 SERVICE_UNAVAILABLE
     * @param latitude the latitude coordinate of the location
     * @param longitude the longitude coordinate of the location
     * @param startTime the trip start time to find the closest weather data for
     * @param trip the trip entity to associate with the meteorological data
     * @return MeteorologicalData entity with weather conditions closest to start time
     * @throws IllegalArgumentException if start time is older than 90 days, coordinates are invalid, or no weather data is available
     * @throws IllegalStateException if Open-Meteo API is unavailable or rate limit is exceeded
     */
    @Cacheable(value = "weatherData", key = "T(java.lang.String).format('%.2f_%.2f_%s', #latitude, #longitude, @openMeteoService.roundToNearestHour(#startTime))", sync = true)
    public MeteorologicalData getWeatherData(Double latitude, Double longitude, LocalDateTime startTime, Trip trip) {
        try {
            // Acquire rate limiter permit (blocks if necessary, throws RequestNotPermitted after timeout)
            RateLimiter.waitForPermission(rateLimiter);
        } catch (RequestNotPermitted e) {
            throw new IllegalStateException("Weather service is temporarily unavailable due to high traffic. Please try again later.", e);
        }
        try {
            validateStartTimeRange(startTime);
            String url = buildWeatherApiUrl(latitude, longitude, startTime);
            String jsonResponse = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);
            OpenMeteoResponse response = responseMapper.fromJsonResponse(jsonResponse);
            int closestIndex = findClosestHourIndex(response.getHourlyData().getTime(), startTime);
            OpenMeteoResponse.HourlyData hourlyData = response.getHourlyData();
            Double temperature = hourlyData.getTemperatureValues().get(closestIndex);
            Integer humidity = hourlyData.getHumidityValues().get(closestIndex);
            Double windSpeed = hourlyData.getWindSpeedValues().get(closestIndex);
            Integer weatherCode = hourlyData.getWeatherCodeValues().get(closestIndex);
            WeatherCondition weatherCondition = WeatherCondition.fromWeatherCode(weatherCode);
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
     * Rounds a LocalDateTime to the nearest hour for cache key generation.
     * This ensures cache keys are aligned with the findClosestHourIndex logic.
     * Rounding logic:
     * - 00-29 minutes: rounds down to current hour
     * - 30-59 minutes: rounds up to next hour
     * Examples:
     * - 14:00 → 14:00
     * - 14:10 → 14:00
     * - 14:29 → 14:00
     * - 14:30 → 15:00
     * - 14:45 → 15:00
     * - 14:59 → 15:00
     * @param dateTime the date and time to round
     * @return LocalDateTime rounded to the nearest hour
     */
    public LocalDateTime roundToNearestHour(LocalDateTime dateTime) {
        int minutes = dateTime.getMinute();
        if (minutes >= 30)
            return dateTime.truncatedTo(ChronoUnit.HOURS).plusHours(1);
        return dateTime.truncatedTo(ChronoUnit.HOURS);
    }

    /**
     * Builds the complete Open-Meteo API URL with all required parameters.
     * Example URL:
     * https://api.open-meteo.com/v1/forecast?latitude=45.3597&longitude=9.3250
     * &hourly=temperature_2m,relative_humidity_2m,wind_speed_10m,weather_code
     * &start_date=2025-11-11&end_date=2025-11-11&timezone=auto
     * @param latitude the latitude coordinate
     * @param longitude the longitude coordinate
     * @param dateTime the date and time to get weather data for
     * @return the complete API URL as a String
     */
    private String buildWeatherApiUrl(Double latitude, Double longitude, LocalDateTime dateTime) {
        String date = dateTime.format(DATE_FORMATTER);
        return openMeteoConfig.getForecastEndpoint() +
                "?latitude=" + latitude +
                "&longitude=" + longitude +
                "&hourly=temperature_2m,relative_humidity_2m,wind_speed_10m,weather_code" +
                "&start_date=" + date +
                "&end_date=" + date +
                "&timezone=" + openMeteoConfig.getTimezone();
    }

    /**
     * Finds the index of the hour in the time array that is closest to the given start time.
     * Uses absolute time difference to find the nearest hour.
     * Algorithm:
     * 1. Parse all timestamp strings to LocalDateTime
     * 2. Calculate absolute time difference between each hour and startTime
     * 3. Return the index with minimum difference
     * Example:
     * - startTime: 2025-11-11T14:30
     * - time array: ["2025-11-11T13:00", "2025-11-11T14:00", "2025-11-11T15:00"]
     * - differences: [90 min, 30 min, 30 min]
     * - returns: index 1 (14:00 - first match with minimum difference)
     * @param timeArray array of ISO 8601 timestamp strings from Open-Meteo
     * @param startTime the trip start time to match against
     * @return the index of the closest hour in the array
     * @throws IllegalArgumentException if no valid timestamp is found
     */
    private int findClosestHourIndex(List<String> timeArray, LocalDateTime startTime) {
        int closestIndex = -1;
        long minDifference = Long.MAX_VALUE;
        for (int i = 0; i < timeArray.size(); i++) {
            try {
                LocalDateTime hourTime = LocalDateTime.parse(timeArray.get(i), TIME_FORMATTER);
                long differenceInMinutes = Math.abs(ChronoUnit.MINUTES.between(startTime, hourTime));
                if (differenceInMinutes < minDifference) {
                    minDifference = differenceInMinutes;
                    closestIndex = i;
                }
            } catch (Exception e) {
                continue;
            }
        }
        if (closestIndex == -1) {
            throw new IllegalArgumentException("No valid weather data found for the trip start time");
        }
        return closestIndex;
    }

    /**
     * Validates that the start time is within the supported historical data range.
     * Open-Meteo Forecast API provides historical data for approximately the last 90 days.
     * If the start time is older than this, weather data cannot be retrieved.
     * @param startTime the trip start time to validate
     * @throws IllegalArgumentException if start time is older than 90 days from now
     */
    private void validateStartTimeRange(LocalDateTime startTime) {
        LocalDateTime now = LocalDateTime.now();
        long daysDifference = ChronoUnit.DAYS.between(startTime, now);
        if (daysDifference > MAX_DAYS_IN_PAST) {
            throw new IllegalArgumentException(String.format("Weather data is not available for trips older than %d days. Trip is %d days old.", MAX_DAYS_IN_PAST, daysDifference));
        }
    }

}