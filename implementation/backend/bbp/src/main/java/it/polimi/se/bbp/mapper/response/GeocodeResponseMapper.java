package it.polimi.se.bbp.mapper.response;

import it.polimi.se.bbp.dto.response.GeocodeResponse;
import it.polimi.se.bbp.dto.result.GeocodeResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * Mapper for converting GeocodeResult to GeocodeResponse DTOs.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface GeocodeResponseMapper {

    /**
     * Converts GeocodeResult to GeocodeResponse DTO.
     * Flattens Coordinate object into separate latitude and longitude fields.
     * @param geocodeResult geocode result from service
     * @return geocode response DTO
     */
    @Mapping(target = "latitude", source = "coordinate.latitude")
    @Mapping(target = "longitude", source = "coordinate.longitude")
    GeocodeResponse toResponse(GeocodeResult geocodeResult);

}