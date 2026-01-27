package it.polimi.se.bbp.mapper.response;

import it.polimi.se.bbp.dto.response.CyclingRoutePointResponse;
import it.polimi.se.bbp.geo.Coordinate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapper for converting Coordinate to RoutePointResponse with sequential position.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CyclingRoutePointResponseMapper {

    /**
     * Converts single Coordinate to RoutePointResponse.
     * @param coordinate coordinate to convert
     * @param sequentialPosition position in route sequence (1-indexed)
     * @return route point response DTO
     */
    @Mapping(target = "latitude", source = "coordinate.latitude")
    @Mapping(target = "longitude", source = "coordinate.longitude")
    @Mapping(target = "sequentialPosition", source = "sequentialPosition")
    CyclingRoutePointResponse toResponse(Coordinate coordinate, Integer sequentialPosition);

    /**
     * Converts list of Coordinates to RoutePointResponse DTOs.
     * Handles iteration and sequential positioning starting from 1.
     * @param coordinates list of route coordinates
     * @return list of route point response DTOs ordered by sequential position
     */
    default List<CyclingRoutePointResponse> toResponses(List<Coordinate> coordinates) {
        if (coordinates == null)
            return new ArrayList<>();
        List<CyclingRoutePointResponse> points = new ArrayList<>(coordinates.size());
        for (int i = 0; i < coordinates.size(); i++) {
            points.add(toResponse(coordinates.get(i), i + 1));
        }
        return points;
    }

}