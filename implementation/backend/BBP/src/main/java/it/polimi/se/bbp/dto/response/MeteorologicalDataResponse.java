package it.polimi.se.bbp.dto.response;

import it.polimi.se.bbp.enums.openmeteo.WeatherCondition;

/**
 * Response for MeteorologicalData containing weather information for a trip.
 * @param weatherCondition weather condition enum (e.g., CLEAR_SKY, RAIN, SNOW)
 * @param weatherDescription human-readable weather description
 * @param temperature ambient temperature in degrees Celsius (range: -99.9 to +99.9)
 * @param humidity relative humidity as percentage (range: 0 to 100)
 * @param windSpeed wind speed in km/h (range: 0.0 to 999.9)
 */
public record MeteorologicalDataResponse(

        /*
         * Weather condition enum.
         * Examples: CLEAR_SKY, RAIN, SNOW.
         */
        WeatherCondition weatherCondition,

        /*
         * Human-readable weather description.
         * Example: "Clear sky", "Rain: Moderate intensity"
         */
        String weatherDescription,

        /*
         * Ambient temperature in degrees Celsius.
         * Range: -99.9 to +99.9
         */
        Double temperature,

        /*
         * Relative humidity as percentage.
         * Range: 0 to 100
         */
        Integer humidity,

        /*
         * Wind speed in km/h.
         * Range: 0.0 to 999.9
         */
        Double windSpeed

) {}