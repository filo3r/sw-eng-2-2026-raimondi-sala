package it.polimi.se.bbp.dto.openmeteo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for Open-Meteo API forecast response.
 * Maps the JSON response from Open-Meteo Forecast API containing hourly weather data.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenMeteoResponse {

    /**
     * Latitude coordinate of the weather data location.
     */
    @JsonProperty("latitude")
    private Double latitude;

    /**
     * Longitude coordinate of the weather data location.
     */
    @JsonProperty("longitude")
    private Double longitude;

    /**
     * Timezone name of the location.
     * Automatically determined when using timezone=auto parameter.
     */
    @JsonProperty("timezone")
    private String timezone;

    /**
     * Hourly weather data containing arrays of time series for various weather parameters.
     * Each array in hourlyData contains values for each hour of the requested period.
     */
    @JsonProperty("hourly")
    private HourlyData hourlyData;

    /**
     * Inner class representing the hourly weather data arrays.
     * Contains time series data for temperature, humidity, wind speed, and weather conditions.
     * All arrays have the same length, with each index representing the same hour.
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HourlyData {

        /**
         * Array of timestamps in ISO 8601 format (e.g., "2025-11-11T00:00", "2025-11-11T01:00").
         * Each timestamp represents the start of an hour.
         */
        @JsonProperty("time")
        private List<String> time;

        /**
         * Array of temperature values at 2 meters above ground in degrees Celsius.
         * Corresponds to each timestamp in the time array.
         */
        @JsonProperty("temperature_2m")
        private List<Double> temperatureValues;

        /**
         * Array of relative humidity values as percentages (0-100).
         * Corresponds to each timestamp in the time array.
         */
        @JsonProperty("relative_humidity_2m")
        private List<Integer> humidityValues;

        /**
         * Array of wind speed values at 10 meters above ground in km/h.
         * Corresponds to each timestamp in the time array.
         */
        @JsonProperty("wind_speed_10m")
        private List<Double> windSpeedValues;

        /**
         * Array of WMO weather codes representing weather conditions.
         * Corresponds to each timestamp in the time array.
         * These codes can be mapped to WeatherCondition enum values.
         */
        @JsonProperty("weather_code")
        private List<Integer> weatherCodeValues;
    }

}