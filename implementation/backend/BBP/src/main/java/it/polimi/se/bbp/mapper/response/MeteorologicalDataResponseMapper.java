package it.polimi.se.bbp.mapper.response;

import it.polimi.se.bbp.dto.response.MeteorologicalDataResponse;
import it.polimi.se.bbp.entity.MeteorologicalData;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * Mapper for converting MeteorologicalData entities to MeteorologicalDataResponse DTOs.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface MeteorologicalDataResponseMapper {

    /**
     * Converts MeteorologicalData entity to MeteorologicalDataResponse DTO.
     * @param meteorologicalData meteorological data entity
     * @return meteorological data response DTO, or null if input is null
     */
    @Mapping(target = "weatherDescription", source = "weatherCondition.weatherDescription")
    MeteorologicalDataResponse toResponse(MeteorologicalData meteorologicalData);

}