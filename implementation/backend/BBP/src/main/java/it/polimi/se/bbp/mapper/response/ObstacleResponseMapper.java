package it.polimi.se.bbp.mapper.response;

import it.polimi.se.bbp.dto.response.ObstacleResponse;
import it.polimi.se.bbp.entity.Obstacle;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

/**
 * Mapper for converting Obstacle entities to ObstacleResponse DTOs.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ObstacleResponseMapper {

    /**
     * Converts Obstacle entity to ObstacleResponse DTO.
     * Extracts descriptions from enums, handles nullable user references.
     * @param obstacle obstacle entity
     * @return obstacle response DTO
     */
    @Mapping(target = "createdById", source = "createdBy.id")
    @Mapping(target = "createdByUsername", source = "createdBy.username")
    @Mapping(target = "updatedById", source = "updatedBy.id")
    @Mapping(target = "updatedByUsername", source = "updatedBy.username")
    @Mapping(target = "typeDescription", source = "type.typeDescription")
    @Mapping(target = "severityDescription", source = "severity.severityDescription")
    ObstacleResponse toResponse(Obstacle obstacle);

    /**
     * Converts list of Obstacle entities to list of ObstacleResponse DTOs.
     * Maintains order of obstacles.
     * @param obstacles list of obstacle entities
     * @return list of obstacle response DTOs
     */
    List<ObstacleResponse> toResponses(List<Obstacle> obstacles);

}