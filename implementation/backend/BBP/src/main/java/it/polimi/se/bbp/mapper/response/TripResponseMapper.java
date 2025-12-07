package it.polimi.se.bbp.mapper.response;

import it.polimi.se.bbp.dto.response.TripResponse;
import it.polimi.se.bbp.entity.Trip;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * Mapper for converting Trip entities to TripResponse DTOs.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        uses = {TripPointResponseMapper.class, MeteorologicalDataResponseMapper.class})
public interface TripResponseMapper {

    /**
     * Converts Trip entity to TripResponse DTO.
     * @param trip trip entity
     * @return trip response DTO
     */
    @Mapping(target = "recordedById", source = "recordedBy.id")
    @Mapping(target = "recordedByUsername", source = "recordedBy.username")
    TripResponse toResponse(Trip trip);

}