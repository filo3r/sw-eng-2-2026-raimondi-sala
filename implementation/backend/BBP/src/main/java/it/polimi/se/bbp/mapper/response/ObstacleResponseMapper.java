package it.polimi.se.bbp.mapper.response;

import it.polimi.se.bbp.dto.response.ObstacleResponse;
import it.polimi.se.bbp.entity.Obstacle;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for converting Obstacle entities to ObstacleResponse DTOs.
 * Handles the transformation of obstacle data for API responses,
 * including the extraction of human-readable descriptions from enums.
 */
@Component
public class ObstacleResponseMapper {

    /**
     * Converts a single Obstacle entity to an ObstacleResponse DTO.
     * Maps all obstacle fields and extracts human-readable descriptions from enums.
     * Handles nullable user references (createdBy, updatedBy) for cases where
     * the user has been deleted from the system.
     * @param obstacle the obstacle entity
     * @return the obstacle response DTO
     */
    public ObstacleResponse toResponse(Obstacle obstacle) {
        return ObstacleResponse.builder()
                .id(obstacle.getId())
                .version(obstacle.getVersion())
                .createdBy(obstacle.getCreatedBy() != null ? obstacle.getCreatedBy().getId() : null)
                .createdAt(obstacle.getCreatedAt())
                .updatedBy(obstacle.getUpdatedBy() != null ? obstacle.getUpdatedBy().getId() : null)
                .updatedAt(obstacle.getUpdatedAt())
                .address(obstacle.getAddress())
                .latitude(obstacle.getLatitude())
                .longitude(obstacle.getLongitude())
                .type(obstacle.getType())
                .typeDescription(obstacle.getType().getTypeDescription())
                .severity(obstacle.getSeverity())
                .severityDescription(obstacle.getSeverity().getSeverityDescription())
                .active(obstacle.getActive())
                .build();
    }

    /**
     * Converts a list of Obstacle entities to a list of ObstacleResponse DTOs.
     * Maintains the order of obstacles as provided in the input list.
     * @param obstacles the list of obstacle entities
     * @return the list of obstacle response DTOs
     */
    public List<ObstacleResponse> toResponses(List<Obstacle> obstacles) {
        return obstacles.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

}