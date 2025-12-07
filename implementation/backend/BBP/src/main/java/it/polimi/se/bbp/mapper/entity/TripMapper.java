package it.polimi.se.bbp.mapper.entity;

import it.polimi.se.bbp.dto.result.GeocodeResult;
import it.polimi.se.bbp.dto.request.TripManualRecordRequest;
import it.polimi.se.bbp.entity.Trip;
import it.polimi.se.bbp.entity.User;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.math.BigDecimal;

/**
 * Mapper for converting trip request data to Trip entities.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        builder = @Builder(disableBuilder = true))
public interface TripMapper {

    /**
     * Converts TripManualRecordRequest to Trip entity.
     * Receives pre-calculated values from service layer.
     * @param request manual trip recording request
     * @param user user recording the trip
     * @param origin geocoded origin location
     * @param destination geocoded destination location
     * @param totalDistanceKm calculated total distance in kilometers
     * @param averageSpeed calculated average speed in km/h
     * @param totalDurationMinutes calculated total duration in minutes
     * @param maxSpeed validated maximum speed in km/h
     * @return trip entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "recordedBy", source = "user")
    @Mapping(target = "origin", source = "origin.address")
    @Mapping(target = "originLatitude", source = "origin.coordinate.latitude")
    @Mapping(target = "originLongitude", source = "origin.coordinate.longitude")
    @Mapping(target = "destination", source = "destination.address")
    @Mapping(target = "destinationLatitude", source = "destination.coordinate.latitude")
    @Mapping(target = "destinationLongitude", source = "destination.coordinate.longitude")
    @Mapping(target = "description", source = "request.description")
    @Mapping(target = "startTime", source = "request.startTime")
    @Mapping(target = "endTime", source = "request.endTime")
    @Mapping(target = "totalDuration", source = "totalDurationMinutes")
    @Mapping(target = "totalDistance", source = "totalDistanceKm")
    @Mapping(target = "averageSpeed", source = "averageSpeed")
    @Mapping(target = "maxSpeed", source = "maxSpeed")
    @Mapping(target = "meteorologicalData", ignore = true)
    @Mapping(target = "tripPoints", ignore = true)
    Trip toEntity(TripManualRecordRequest request, User user, GeocodeResult origin, GeocodeResult destination,
                  int totalDurationMinutes, BigDecimal totalDistanceKm, BigDecimal averageSpeed, BigDecimal maxSpeed);

}