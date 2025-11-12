package it.polimi.se.bbp.mapper.response;

import it.polimi.se.bbp.dto.response.TripPointResponse;
import it.polimi.se.bbp.entity.TripPoint;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for converting TripPoint entities to TripPointResponse DTOs.
 */
@Component
public class TripPointResponseMapper {

    /**
     * Converts a single TripPoint entity to a TripPointResponse DTO.
     * @param tripPoint the trip point entity
     * @return the trip point response DTO
     */
    public TripPointResponse toResponse(TripPoint tripPoint) {
        return TripPointResponse.builder()
                .latitude(tripPoint.getLatitude())
                .longitude(tripPoint.getLongitude())
                .sequentialPosition(tripPoint.getSequentialPosition())
                .timestamp(tripPoint.getTimestamp())
                .build();
    }

    /**
     * Converts a list of TripPoint entities to a list of TripPointResponse DTOs.
     * @param tripPoints the list of trip point entities
     * @return the list of trip point response DTOs
     */
    public List<TripPointResponse> toResponses(List<TripPoint> tripPoints) {
        return tripPoints.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

}