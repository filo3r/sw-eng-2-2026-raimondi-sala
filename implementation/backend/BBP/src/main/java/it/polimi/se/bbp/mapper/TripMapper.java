package it.polimi.se.bbp.mapper;

import it.polimi.se.bbp.dto.mapbox.GeocodeResult;
import it.polimi.se.bbp.dto.request.TripManualRecordRequest;
import it.polimi.se.bbp.entity.Trip;
import it.polimi.se.bbp.entity.User;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;

/**
 * Mapper for converting trip request data to Trip entities.
 */
@Component
public class TripMapper {

    /**
     * Converts a TripManualRecordRequest to a Trip entity.
     * Receives pre-calculated values from the service layer.
     * @param request the manual trip recording request
     * @param user the user recording the trip
     * @param origin the geocoded origin location
     * @param destination the geocoded destination location
     * @param totalDistanceKm the calculated total distance in kilometers
     * @param averageSpeed the calculated average speed in km/h
     * @param totalDurationMinutes the calculated total duration in minutes
     * @param maxSpeed the validated maximum speed in km/h
     * @return the trip entity (without trip points)
     */
    public Trip toEntity(TripManualRecordRequest request, User user, GeocodeResult origin, GeocodeResult destination,
                         BigDecimal totalDistanceKm, BigDecimal averageSpeed,
                         int totalDurationMinutes, BigDecimal maxSpeed) {
        return Trip.builder()
                .recordedBy(user)
                .origin(origin.getAddress())
                .originLatitude(origin.getCoordinate().getLatitude())
                .originLongitude(origin.getCoordinate().getLongitude())
                .destination(destination.getAddress())
                .destinationLatitude(destination.getCoordinate().getLatitude())
                .destinationLongitude(destination.getCoordinate().getLongitude())
                .description(request.getDescription())
                .tripDate(request.getStartTime().toLocalDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .totalDuration(totalDurationMinutes)
                .totalDistance(totalDistanceKm)
                .averageSpeed(averageSpeed)
                .maxSpeed(maxSpeed)
                .tripPoints(new ArrayList<>())
                .build();
    }

}