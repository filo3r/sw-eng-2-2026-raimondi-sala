package it.polimi.se.bbp.mapper.entity;

import it.polimi.se.bbp.dto.mapbox.GeocodeResult;
import it.polimi.se.bbp.dto.request.BikePathManualCreateRequest;
import it.polimi.se.bbp.entity.BikePath;
import it.polimi.se.bbp.entity.User;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;

/**
 * Mapper for converting bike path request data to BikePath entities.
 */
@Component
public class BikePathMapper {

    /**
     * Converts a BikePathManualCreateRequest to a BikePath entity.
     * Receives pre-calculated values from the service layer.
     * @param request the bike path creation request
     * @param createdBy the user creating the bike path
     * @param origin the geocoded origin location
     * @param destination the geocoded destination location
     * @param totalDistanceKm the calculated total distance in kilometers
     * @param score the calculated quality score (0.0 - 5.0)
     * @param createdAt the creation timestamp
     * @return the bike path entity with empty collections for points and obstacles
     */
    public BikePath toEntity(BikePathManualCreateRequest request, User createdBy, OffsetDateTime createdAt, User updatedBy, OffsetDateTime updatedAt, GeocodeResult origin, GeocodeResult destination, BigDecimal score, BigDecimal totalDistanceKm) {
        return BikePath.builder()
                .createdBy(createdBy)
                .createdAt(createdAt)
                .updatedBy(updatedBy)
                .updatedAt(updatedAt)
                .origin(origin.getAddress())
                .originLatitude(origin.getCoordinate().getLatitude())
                .originLongitude(origin.getCoordinate().getLongitude())
                .destination(destination.getAddress())
                .destinationLatitude(destination.getCoordinate().getLatitude())
                .destinationLongitude(destination.getCoordinate().getLongitude())
                .description(request.getDescription())
                .score(score)
                .status(request.getStatus())
                .totalDistance(totalDistanceKm)
                .published(request.getPublished())
                .bikePathPoints(new ArrayList<>())
                .obstacles(new ArrayList<>())
                .build();
    }

}