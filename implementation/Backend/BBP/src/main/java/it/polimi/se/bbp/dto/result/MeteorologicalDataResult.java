package it.polimi.se.bbp.dto.result;

import it.polimi.se.bbp.enums.openmeteo.WeatherCondition;

/**
 * Result of meteorological data retrieval containing weather conditions.
 * @param weatherCondition weather condition enum
 * @param temperature temperature in degrees Celsius
 * @param humidity relative humidity as percentage
 * @param windSpeed wind speed in km/h
 */
public record MeteorologicalDataResult (

        /*
         * Weather condition enum.
         */
        WeatherCondition weatherCondition,

        /*
         * Temperature in degrees Celsius.
         */
        Double temperature,

        /*
         * Relative humidity as percentage.
         */
        Integer humidity,

        /*
         * Wind speed in km/h.
         */
        Double windSpeed

) {}