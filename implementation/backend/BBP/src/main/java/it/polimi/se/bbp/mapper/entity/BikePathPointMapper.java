package it.polimi.se.bbp.mapper.entity;

import it.polimi.se.bbp.dto.mapbox.Coordinate;
import it.polimi.se.bbp.entity.BikePath;
import it.polimi.se.bbp.entity.BikePathPoint;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapper for converting route coordinates to BikePathPoint entities.
 */
@Component
public class BikePathPointMapper {

    /**
     * Converts a single route coordinate to a BikePathPoint entity.
     * Timestamp is set to null for manually created bike paths.
     * For GPS-tracked bike paths (future functionality), timestamp should be provided separately.
     * @param coordinate the route coordinate from MapboxService
     * @param bikePath the bike path entity to associate with this point
     * @param sequentialPosition the sequential position in the route (1-indexed)
     * @return bike path point entity
     */
    public BikePathPoint toEntity(Coordinate coordinate, BikePath bikePath, int sequentialPosition) {
        return BikePathPoint.builder()
                .bikePath(bikePath)
                .latitude(coordinate.getLatitude())
                .longitude(coordinate.getLongitude())
                .timestamp(null) // null for manually created bike paths
                .sequentialPosition(sequentialPosition)
                .build();
    }

    /**
     * Converts a list of route coordinates to BikePathPoint entities.
     * Each coordinate is assigned a sequential position (1-indexed).
     * Timestamp is set to null for manually created bike paths.
     * The sequential position allows reconstruction of the route in the correct order.
     * @param coordinates the list of route coordinates from MapboxService
     * @param bikePath the bike path entity to associate with these points
     * @return list of bike path point entities ordered by sequential position
     */
    public List<BikePathPoint> toEntities(List<Coordinate> coordinates, BikePath bikePath) {
        List<BikePathPoint> bikePathPoints = new ArrayList<>();
        for (int i = 0; i < coordinates.size(); i++) {
            BikePathPoint bikePathPoint = toEntity(coordinates.get(i), bikePath, i + 1);
            bikePathPoints.add(bikePathPoint);
        }
        return bikePathPoints;
    }

}