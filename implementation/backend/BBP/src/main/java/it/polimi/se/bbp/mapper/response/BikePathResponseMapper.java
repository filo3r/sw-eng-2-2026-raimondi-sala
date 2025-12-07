package it.polimi.se.bbp.mapper.response;

import it.polimi.se.bbp.dto.response.BikePathResponse;
import it.polimi.se.bbp.entity.BikePath;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * Mapper for converting BikePath entities to BikePathResponse DTOs.
 * Orchestrates mapping of bike path data with nested collections.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        uses = {BikePathPointResponseMapper.class, ObstacleResponseMapper.class})
public interface BikePathResponseMapper {

    /**
     * Converts BikePath entity to BikePathResponse DTO.
     * Maps all fields including nested collections (points and obstacles).
     * Extracts descriptions from enums, handles nullable updatedBy reference.
     * @param bikePath bike path entity
     * @return bike path response DTO with complete route and obstacle information
     */
    @Mapping(target = "createdById", source = "createdBy.id")
    @Mapping(target = "createdByUsername", source = "createdBy.username")
    @Mapping(target = "updatedById", source = "updatedBy.id")
    @Mapping(target = "updatedByUsername", source = "updatedBy.username")
    @Mapping(target = "statusDescription", source = "status.statusDescription")
    BikePathResponse toResponse(BikePath bikePath);

}