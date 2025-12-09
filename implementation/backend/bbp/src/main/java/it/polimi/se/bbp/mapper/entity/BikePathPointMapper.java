package it.polimi.se.bbp.mapper.entity;

import it.polimi.se.bbp.geo.Coordinate;
import it.polimi.se.bbp.entity.BikePath;
import it.polimi.se.bbp.entity.BikePathPoint;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.time.OffsetDateTime;
import java.util.*;

/**
 * Mapper for converting route coordinates to BikePathPoint entities.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        builder = @Builder(disableBuilder = true))
public interface BikePathPointMapper {

    /**
     * Converts single route coordinate to BikePathPoint entity.
     * @param coordinate route coordinate from MapboxService
     * @param bikePath bike path entity to associate with this point
     * @param timestamp optional timestamp for this point
     * @param sequentialPosition sequential position in route (1-indexed)
     * @return bike path point entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "bikePath", source = "bikePath")
    @Mapping(target = "latitude", source = "coordinate.latitude")
    @Mapping(target = "longitude", source = "coordinate.longitude")
    @Mapping(target = "timestamp", source = "timestamp")
    @Mapping(target = "sequentialPosition", source = "sequentialPosition")
    BikePathPoint toEntity(Coordinate coordinate, BikePath bikePath, OffsetDateTime timestamp, int sequentialPosition);

    /**
     * Converts list of route coordinates to list of BikePathPoint entities.
     * Handles iteration and sequential numbering, preserving order.
     * @param coordinates list of route coordinates
     * @param bikePath bike path entity
     * @param timestamps optional list of timestamps (can be null or shorter than coordinates)
     * @return list of bike path point entities ordered by sequential position
     */
    default List<BikePathPoint> toEntities(List<Coordinate> coordinates, BikePath bikePath, List<OffsetDateTime> timestamps) {
        if (coordinates == null)
            return new ArrayList<>();
        List<BikePathPoint> points = new ArrayList<>(coordinates.size());
        for (int i = 0; i < coordinates.size(); i++) {
            OffsetDateTime ts = (timestamps != null && i < timestamps.size()) ? timestamps.get(i) : null;
            points.add(toEntity(coordinates.get(i), bikePath, ts, i + 1));
        }
        return points;
    }

}