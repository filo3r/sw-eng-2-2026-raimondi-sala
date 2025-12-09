package it.polimi.se.bbp.dto.openmeteo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.polimi.se.bbp.exception.openmeteo.OpenMeteoApiException;

import java.util.List;

/**
 * Response from Open-Meteo Forecast API containing hourly weather data.
 * Includes self-validation logic to ensure data consistency.
 * @param latitude latitude coordinate of weather data location
 * @param longitude longitude coordinate of weather data location
 * @param timezone timezone name of location (auto-determined by API)
 * @param hourlyData hourly weather data containing time series arrays
 * @throws OpenMeteoApiException if hourly data is null
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenMeteoResponse(

        /* Latitude coordinate of weather data location. */
        @JsonProperty("latitude")
        Double latitude,

        /* Longitude coordinate of weather data location. */
        @JsonProperty("longitude")
        Double longitude,

        /* Timezone name of location (auto-determined by API). */
        @JsonProperty("timezone")
        String timezone,

        /* Hourly weather data containing time series arrays. Validated to be non-null. */
        @JsonProperty("hourly")
        HourlyData hourlyData

) {

    /**
     * Validates that hourly data is present in response.
     * @throws OpenMeteoApiException if hourly data is null
     */
    public OpenMeteoResponse {
        if (hourlyData == null)
            throw new OpenMeteoApiException("No weather data available (hourlyData is null)");
    }

    /**
     * Hourly weather data containing time series arrays.
     * All arrays must have same length, with each index representing same hour.
     * @param time array of timestamps in ISO 8601 format (e.g., "2025-11-13T14:00")
     * @param temperatureValues array of temperature values at 2m above ground (°C)
     * @param humidityValues array of relative humidity values at 2m above ground (%)
     * @param windSpeedValues array of wind speed values at 10m above ground (km/h)
     * @param weatherCodeValues array of WMO weather codes
     * @throws OpenMeteoApiException if any array is null/empty or arrays have inconsistent lengths
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HourlyData(

            /* Array of timestamps in ISO 8601 format. Time index for all other arrays. */
            @JsonProperty("time")
            List<String> time,

            /* Array of temperature values at 2m above ground (°C). */
            @JsonProperty("temperature_2m")
            List<Double> temperatureValues,

            /* Array of relative humidity values at 2m above ground (%). */
            @JsonProperty("relative_humidity_2m")
            List<Integer> humidityValues,

            /* Array of wind speed values at 10m above ground (km/h). */
            @JsonProperty("wind_speed_10m")
            List<Double> windSpeedValues,

            /* Array of WMO weather codes representing weather conditions. */
            @JsonProperty("weather_code")
            List<Integer> weatherCodeValues

    ) {

        /**
         * Validates all required arrays are present, non-empty, and have consistent lengths.
         * @throws OpenMeteoApiException if any array is null/empty or arrays have inconsistent lengths
         */
        public HourlyData {
            // Validate existence of required data lists
            if (time == null || time.isEmpty())
                throw new OpenMeteoApiException("Missing 'time' data");
            if (temperatureValues == null || temperatureValues.isEmpty())
                throw new OpenMeteoApiException("Missing 'temperature_2m' data");
            if (humidityValues == null || humidityValues.isEmpty())
                throw new OpenMeteoApiException("Missing 'relative_humidity_2m' data");
            if (windSpeedValues == null || windSpeedValues.isEmpty())
                throw new OpenMeteoApiException("Missing 'wind_speed_10m' data");
            if (weatherCodeValues == null || weatherCodeValues.isEmpty())
                throw new OpenMeteoApiException("Missing 'weather_code' data");
            // Validate data consistency (all arrays must have the same length)
            int expectedSize = time.size();
            if (temperatureValues.size() != expectedSize || humidityValues.size() != expectedSize ||
                    windSpeedValues.size() != expectedSize || weatherCodeValues.size() != expectedSize) {
                throw new OpenMeteoApiException(
                        String.format("Inconsistent data arrays length. Time: %d, Temp: %d, Hum: %d, Wind: %d, Code: %d",
                                expectedSize, temperatureValues.size(), humidityValues.size(),
                                windSpeedValues.size(), weatherCodeValues.size()));
            }
        }

    }

}