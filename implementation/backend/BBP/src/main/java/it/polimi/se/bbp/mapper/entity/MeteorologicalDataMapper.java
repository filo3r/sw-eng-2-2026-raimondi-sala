package it.polimi.se.bbp.mapper.entity;

import it.polimi.se.bbp.entity.MeteorologicalData;
import it.polimi.se.bbp.entity.Trip;
import it.polimi.se.bbp.enums.openmeteo.WeatherCondition;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * Mapper for converting weather data parameters to MeteorologicalData entities.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        builder = @Builder(disableBuilder = true))
public interface MeteorologicalDataMapper {

    /**
     * Converts weather data parameters to MeteorologicalData entity.
     * @param trip trip entity to associate with meteorological data
     * @param weatherCondition weather condition enum
     * @param temperature temperature in degrees Celsius
     * @param humidity relative humidity percentage
     * @param windSpeed wind speed in km/h
     * @return meteorological data entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "trip", source = "trip")
    @Mapping(target = "weatherCondition", source = "weatherCondition")
    @Mapping(target = "temperature", source = "temperature")
    @Mapping(target = "humidity", source = "humidity")
    @Mapping(target = "windSpeed", source = "windSpeed")
    MeteorologicalData toEntity(Trip trip, WeatherCondition weatherCondition, Double temperature, Integer humidity, Double windSpeed);

}