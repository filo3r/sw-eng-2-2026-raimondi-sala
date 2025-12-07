package it.polimi.se.bbp.mapper.entity;

import it.polimi.se.bbp.dto.result.GeocodeResult;
import it.polimi.se.bbp.dto.request.ObstacleCreateRequest;
import it.polimi.se.bbp.entity.BikePath;
import it.polimi.se.bbp.entity.Obstacle;
import it.polimi.se.bbp.entity.User;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Mapper for converting obstacle request data to Obstacle entities.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        builder = @Builder(disableBuilder = true))
public interface ObstacleMapper {

    /**
     * Converts ObstacleCreateRequest to Obstacle entity.
     * @param request obstacle creation request
     * @param bikePath bike path entity to associate with obstacle
     * @param createdBy user creating the obstacle
     * @param createdAt creation timestamp
     * @param updatedBy user updating the obstacle (can be null)
     * @param updatedAt update timestamp (can be null)
     * @param geocodeResult geocoded location of the obstacle
     * @param active whether the obstacle is active
     * @param positionOnPath position on bike path
     * @return obstacle entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "bikePath", source = "bikePath")
    @Mapping(target = "createdBy", source = "createdBy")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedBy", source = "updatedBy")
    @Mapping(target = "updatedAt", source = "updatedAt")
    @Mapping(target = "address", source = "geocodeResult.address")
    @Mapping(target = "latitude", source = "geocodeResult.coordinate.latitude")
    @Mapping(target = "longitude", source = "geocodeResult.coordinate.longitude")
    @Mapping(target = "type", source = "request.type")
    @Mapping(target = "severity", source = "request.severity")
    @Mapping(target = "active", source = "active")
    @Mapping(target = "positionOnPath", source = "positionOnPath")
    Obstacle toEntity(ObstacleCreateRequest request, BikePath bikePath, User createdBy, OffsetDateTime createdAt, User updatedBy,
                      OffsetDateTime updatedAt, GeocodeResult geocodeResult, Boolean active, Integer positionOnPath);

    /**
     * Converts list of ObstacleCreateRequests to Obstacle entities.
     * Each obstacle receives its corresponding position from the positionsOnPath list.
     * @param requests list of obstacle creation requests
     * @param geocodeResults list of geocoded locations (must match requests size)
     * @param bikePath bike path entity to associate with obstacles
     * @param createdBy user creating the obstacles
     * @param createdAt creation timestamp
     * @param updatedBy user updating the obstacles (can be null)
     * @param updatedAt update timestamp (can be null)
     * @param active whether the obstacles are active
     * @param positionsOnPath list of position values for obstacles (must match requests size)
     * @return list of obstacle entities
     */
    default List<Obstacle> toEntities(List<ObstacleCreateRequest> requests, List<GeocodeResult> geocodeResults,
                                      BikePath bikePath, User createdBy, OffsetDateTime createdAt, User updatedBy,
                                      OffsetDateTime updatedAt, Boolean active, List<Integer> positionsOnPath) {
        if (requests == null || requests.isEmpty() || geocodeResults == null || geocodeResults.size() != requests.size()
                || positionsOnPath == null || positionsOnPath.size() != requests.size())
            return new ArrayList<>();
        List<Obstacle> obstacles = new ArrayList<>(requests.size());
        for (int i = 0; i < requests.size(); i++) {
            Obstacle obstacle = toEntity(requests.get(i), bikePath, createdBy, createdAt, updatedBy, updatedAt,
                    geocodeResults.get(i), active, positionsOnPath.get(i));
            obstacles.add(obstacle);
        }
        return obstacles;
    }

}