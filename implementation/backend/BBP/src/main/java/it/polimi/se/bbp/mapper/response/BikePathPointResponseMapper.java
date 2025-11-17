package it.polimi.se.bbp.mapper.response;

import it.polimi.se.bbp.dto.response.BikePathPointResponse;
import it.polimi.se.bbp.entity.BikePathPoint;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for converting BikePathPoint entities to BikePathPointResponse DTOs.
 * Handles the transformation of bike path point data for API responses.
 */
@Component
public class BikePathPointResponseMapper {

    /**
     * Converts a single BikePathPoint entity to a BikePathPointResponse DTO.
     * Maps all coordinate and metadata fields from the entity to the response format.
     * @param bikePathPoint the bike path point entity
     * @return the bike path point response DTO
     */
    public BikePathPointResponse toResponse(BikePathPoint bikePathPoint) {
        return BikePathPointResponse.builder()
                .latitude(bikePathPoint.getLatitude())
                .longitude(bikePathPoint.getLongitude())
                .timestamp(bikePathPoint.getTimestamp())
                .sequentialPosition(bikePathPoint.getSequentialPosition())
                .build();
    }

    /**
     * Converts a list of BikePathPoint entities to a list of BikePathPointResponse DTOs.
     * Maintains the order of points as provided in the input list.
     * @param bikePathPoints the list of bike path point entities
     * @return the list of bike path point response DTOs
     */
    public List<BikePathPointResponse> toResponses(List<BikePathPoint> bikePathPoints) {
        return bikePathPoints.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

}