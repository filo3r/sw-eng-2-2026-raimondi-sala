package it.polimi.se.bbp.mapper;

import it.polimi.se.bbp.dto.response.TripResponse;
import it.polimi.se.bbp.entity.Trip;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting Trip entities to TripResponse DTOs.
 */
@Component
@RequiredArgsConstructor
public class TripResponseMapper {

    /**
     * Mapper for converting TripPoint entities to TripPointResponse DTOs.
     */
    private final TripPointResponseMapper tripPointResponseMapper;

    /**
     * Converts a Trip entity to a TripResponse DTO.
     * @param trip the trip entity
     * @return the trip response DTO
     */
    public TripResponse toResponse(Trip trip) {
        return TripResponse.builder()
                .id(trip.getId())
                .recordedBy(trip.getRecordedBy().getId())
                .origin(trip.getOrigin())
                .originLatitude(trip.getOriginLatitude())
                .originLongitude(trip.getOriginLongitude())
                .destination(trip.getDestination())
                .destinationLatitude(trip.getDestinationLatitude())
                .destinationLongitude(trip.getDestinationLongitude())
                .description(trip.getDescription())
                .tripDate(trip.getTripDate())
                .startTime(trip.getStartTime())
                .endTime(trip.getEndTime())
                .totalDuration(trip.getTotalDuration())
                .totalDistance(trip.getTotalDistance())
                .averageSpeed(trip.getAverageSpeed())
                .maxSpeed(trip.getMaxSpeed())
                .tripPoints(tripPointResponseMapper.toResponses(trip.getTripPoints()))
                .build();
    }

}