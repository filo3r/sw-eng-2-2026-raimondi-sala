package it.polimi.se.bbp.mapper.response;

import it.polimi.se.bbp.dto.response.BikePathResponse;
import it.polimi.se.bbp.entity.BikePath;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting BikePath entities to BikePathResponse DTOs.
 * Orchestrates the mapping of bike path data along with its nested collections
 * of bike path points and obstacles.
 */
@Component
@RequiredArgsConstructor
public class BikePathResponseMapper {

    /**
     * Mapper for converting BikePathPoint entities to BikePathPointResponse DTOs.
     */
    private final BikePathPointResponseMapper bikePathPointResponseMapper;

    /**
     * Mapper for converting Obstacle entities to ObstacleResponse DTOs.
     */
    private final ObstacleResponseMapper obstacleResponseMapper;

    /**
     * Converts a BikePath entity to a BikePathResponse DTO.
     * Maps all bike path fields including nested collections (points and obstacles).
     * Extracts human-readable descriptions from enums.
     * Handles nullable user references (updatedBy) for cases where the user has been deleted.
     * @param bikePath the bike path entity
     * @return the bike path response DTO with complete route and obstacle information
     */
    public BikePathResponse toResponse(BikePath bikePath) {
        return BikePathResponse.builder()
                .id(bikePath.getId())
                .version(bikePath.getVersion())
                .createdBy(bikePath.getCreatedBy().getId())
                .createdAt(bikePath.getCreatedAt())
                .updatedBy(bikePath.getUpdatedBy() != null ? bikePath.getUpdatedBy().getId() : null)
                .updatedAt(bikePath.getUpdatedAt())
                .origin(bikePath.getOrigin())
                .originLatitude(bikePath.getOriginLatitude())
                .originLongitude(bikePath.getOriginLongitude())
                .destination(bikePath.getDestination())
                .destinationLatitude(bikePath.getDestinationLatitude())
                .destinationLongitude(bikePath.getDestinationLongitude())
                .description(bikePath.getDescription())
                .score(bikePath.getScore())
                .status(bikePath.getStatus())
                .statusDescription(bikePath.getStatus().getStatusDescription())
                .totalDistance(bikePath.getTotalDistance())
                .published(bikePath.getPublished())
                .bikePathPoints(bikePathPointResponseMapper.toResponses(bikePath.getBikePathPoints()))
                .obstacles(obstacleResponseMapper.toResponses(bikePath.getObstacles()))
                .build();
    }

}