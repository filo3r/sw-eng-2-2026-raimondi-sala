package it.polimi.se.bbp.mapper.response;

import it.polimi.se.bbp.dto.response.CyclingRouteResponse;
import it.polimi.se.bbp.dto.result.CyclingRouteResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * Mapper for converting CyclingRouteResult to RouteResponse DTOs.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        uses = {CyclingRoutePointResponseMapper.class})
public interface CyclingRouteResponseMapper {

    /**
     * Converts CyclingRouteResult to RouteResponse DTO.
     * Maps coordinates to ordered points with sequential positions.
     * @param result cycling route result from service
     * @return route response DTO with ordered points
     */
    @Mapping(target = "points", source = "routeCoordinates")
    CyclingRouteResponse toResponse(CyclingRouteResult result);

}