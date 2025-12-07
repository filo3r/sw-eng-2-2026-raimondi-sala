package it.polimi.se.bbp.mapper.entity;

import it.polimi.se.bbp.geo.Coordinate;
import it.polimi.se.bbp.entity.Trip;
import it.polimi.se.bbp.entity.TripPoint;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.time.OffsetDateTime;
import java.util.*;

/**
 * Mapper for converting route coordinates to TripPoint entities.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        builder = @Builder(disableBuilder = true))
public interface TripPointMapper {

    /**
     * Converts single route coordinate to TripPoint entity.
     * @param coordinate route coordinate from MapboxService
     * @param trip trip entity to associate with this point
     * @param timestamp optional timestamp for this point
     * @param sequentialPosition sequential position in route (1-indexed)
     * @return trip point entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "trip", source = "trip")
    @Mapping(target = "latitude", source = "coordinate.latitude")
    @Mapping(target = "longitude", source = "coordinate.longitude")
    @Mapping(target = "timestamp", source = "timestamp")
    @Mapping(target = "sequentialPosition", source = "sequentialPosition")
    TripPoint toEntity(Coordinate coordinate, Trip trip, OffsetDateTime timestamp, int sequentialPosition);

    /**
     * Converts list of route coordinates to TripPoint entities.
     * Handles iteration and sequential positioning, preserving order.
     * @param coordinates list of route coordinates
     * @param trip trip entity
     * @param timestamps optional list of timestamps (can be null or empty)
     * @return list of trip point entities ordered by sequential position
     */
    default List<TripPoint> toEntities(List<Coordinate> coordinates, Trip trip, List<OffsetDateTime> timestamps) {
        if (coordinates == null)
            return new ArrayList<>();
        List<TripPoint> tripPoints = new ArrayList<>(coordinates.size());
        for (int i = 0; i < coordinates.size(); i++) {
            OffsetDateTime ts = (timestamps != null && i < timestamps.size()) ? timestamps.get(i) : null;
            tripPoints.add(toEntity(coordinates.get(i), trip, ts, i + 1));
        }
        return tripPoints;
    }

}