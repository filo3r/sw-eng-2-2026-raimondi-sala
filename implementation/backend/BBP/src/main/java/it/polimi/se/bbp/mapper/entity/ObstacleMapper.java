package it.polimi.se.bbp.mapper.entity;

import it.polimi.se.bbp.dto.mapbox.GeocodeResult;
import it.polimi.se.bbp.dto.request.ObstacleCreateRequest;
import it.polimi.se.bbp.entity.BikePath;
import it.polimi.se.bbp.entity.Obstacle;
import it.polimi.se.bbp.entity.User;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * Mapper for converting obstacle request data to Obstacle entities.
 */
@Component
public class ObstacleMapper {

    /**
     * Converts an ObstacleCreateRequest to an Obstacle entity.
     * Receives geocoded location data from the service layer.
     * The obstacle is always created with active=true.
     * @param request the obstacle creation request
     * @param bikePath the bike path entity to associate with this obstacle
     * @param geocodeResult the geocoded location of the obstacle (address and coordinates)
     * @param createdBy the user creating the obstacle
     * @param createdAt the creation timestamp
     * @return the obstacle entity with active=true and no update information
     */
    public Obstacle toEntity(ObstacleCreateRequest request, BikePath bikePath, User createdBy, OffsetDateTime createdAt, User updatedBy, OffsetDateTime updatedAt, GeocodeResult geocodeResult, Boolean active) {
        return Obstacle.builder()
                .bikePath(bikePath)
                .createdBy(createdBy)
                .createdAt(createdAt)
                .updatedBy(updatedBy)
                .updatedAt(updatedAt)
                .address(geocodeResult.getAddress())
                .latitude(geocodeResult.getCoordinate().getLatitude())
                .longitude(geocodeResult.getCoordinate().getLongitude())
                .type(request.getType())
                .severity(request.getSeverity())
                .active(active)
                .build();
    }

}