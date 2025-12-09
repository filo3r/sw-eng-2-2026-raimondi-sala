package it.polimi.se.bbp.mapper.response;

import it.polimi.se.bbp.dto.response.BikePathPointResponse;
import it.polimi.se.bbp.entity.BikePathPoint;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

/**
 * Mapper for converting BikePathPoint entities to BikePathPointResponse DTOs.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface BikePathPointResponseMapper {

    /**
     * Converts BikePathPoint entity to BikePathPointResponse DTO.
     * MapStruct automatically maps fields with matching names.
     * @param bikePathPoint bike path point entity
     * @return bike path point response DTO
     */
    BikePathPointResponse toResponse(BikePathPoint bikePathPoint);

    /**
     * Converts list of BikePathPoint entities to list of BikePathPointResponse DTOs.
     * Maintains order of points, MapStruct generates loop logic automatically.
     * @param bikePathPoints list of bike path point entities
     * @return list of bike path point response DTOs
     */
    List<BikePathPointResponse> toResponses(List<BikePathPoint> bikePathPoints);

}