package it.polimi.se.bbp.mapper.entity;

import it.polimi.se.bbp.entity.MeteorologicalData;
import it.polimi.se.bbp.entity.Trip;
import it.polimi.se.bbp.enums.openmeteo.WeatherCondition;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting weather data parameters to MeteorologicalData entities.
 */
@Component
public class MeteorologicalDataMapper {

    /**
     * Converts weather data parameters to a MeteorologicalData entity.
     * @param trip the trip entity to associate with the meteorological data
     * @param weatherCondition the weather condition enum
     * @param temperature the temperature in degrees Celsius
     * @param humidity the relative humidity percentage
     * @param windSpeed the wind speed in km/h
     * @return the meteorological data entity
     */
    public MeteorologicalData toEntity(Trip trip, WeatherCondition weatherCondition, Double temperature, Integer humidity, Double windSpeed) {
        return MeteorologicalData.builder()
                .trip(trip)
                .weatherCondition(weatherCondition)
                .temperature(temperature)
                .humidity(humidity)
                .windSpeed(windSpeed)
                .build();
    }

}