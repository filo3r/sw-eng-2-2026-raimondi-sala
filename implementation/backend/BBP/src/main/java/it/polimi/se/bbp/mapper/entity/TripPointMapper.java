package it.polimi.se.bbp.mapper.entity;

import it.polimi.se.bbp.dto.mapbox.Coordinate;
import it.polimi.se.bbp.entity.Trip;
import it.polimi.se.bbp.entity.TripPoint;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Mapper for converting route coordinates to TripPoint entities.
 */
@Component
public class TripPointMapper {

    /**
     * Converts a single route coordinate to a TripPoint entity.
     * Timestamp is set to null for manually recorded trips.
     * @param coordinate the route coordinate from MapboxService
     * @param trip the trip entity to associate with this point
     * @param sequentialPosition the sequential position in the route (1-indexed)
     * @return trip point entity
     */
    public TripPoint toEntity(Coordinate coordinate, Trip trip, OffsetDateTime timestamp, int sequentialPosition) {
        return TripPoint.builder()
                .trip(trip)
                .latitude(coordinate.getLatitude())
                .longitude(coordinate.getLongitude())
                .timestamp(timestamp)
                .sequentialPosition(sequentialPosition)
                .build();
    }

    /**
     * Converts a list of route coordinates to TripPoint entities.
     * Each coordinate is assigned a sequential position (1-indexed).
     * Timestamp is set to null for manually recorded trips.
     * @param coordinates the list of route coordinates from MapboxService
     * @param trip the trip entity to associate with these points
     * @return list of trip point entities
     */
    public List<TripPoint> toEntities(List<Coordinate> coordinates, Trip trip, List<OffsetDateTime> timestamps) {
        List<TripPoint> tripPoints = new ArrayList<>();
        for (int i = 0; i < coordinates.size(); i++) {
            OffsetDateTime timestamp = (timestamps != null && i < timestamps.size()) ? timestamps.get(i) : null;
            TripPoint tripPoint = toEntity(coordinates.get(i), trip, timestamp, i + 1);
            tripPoints.add(tripPoint);
        }
        return tripPoints;
    }

}