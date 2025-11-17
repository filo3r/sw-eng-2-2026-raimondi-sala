package it.polimi.se.bbp.dto.response;

import it.polimi.se.bbp.enums.BikePathStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * DTO for BikePath response.
 * Contains all bike path information including route details, status, quality score,
 * GPS coordinates, and associated obstacles.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BikePathResponse {

    /**
     * Unique identifier of the bike path.
     */
    private Long id;

    /**
     *
     */
    private Long version;

    /**
     * ID of the user who originally created this bike path.
     */
    private Long createdBy;

    /**
     * Date and time when this bike path was created.
     */
    private OffsetDateTime createdAt;

    /**
     * ID of the user who last updated this bike path.
     * Will be null if the bike path has never been updated or if the user who updated it has been deleted.
     */
    private Long updatedBy;

    /**
     * Date and time of the last update to this bike path.
     * Will be null if the bike path has never been updated.
     */
    private OffsetDateTime updatedAt;

    /**
     * Formatted address of the bike path's starting point.
     */
    private String origin;

    /**
     * Latitude of the origin point.
     */
    private Double originLatitude;

    /**
     * Longitude of the origin point.
     */
    private Double originLongitude;

    /**
     * Formatted address of the bike path's destination point.
     */
    private String destination;

    /**
     * Latitude of the destination point.
     */
    private Double destinationLatitude;

    /**
     * Longitude of the destination point.
     */
    private Double destinationLongitude;

    /**
     * Optional description or notes about the bike path.
     */
    private String description;

    /**
     * Overall quality score of the bike path.
     * Range: 0.0 to 5.0
     * Computed based on the path status, obstacles, and other factors.
     */
    private BigDecimal score;

    /**
     * Current condition status of the bike path.
     * Enum indicating the maintenance level and usability.
     */
    private BikePathStatus status;

    /**
     * Human-readable description of the bike path status.
     * Example: "Excellent", "Good", "Under Maintenance"
     */
    private String statusDescription;

    /**
     * Total distance of the bike path in kilometers.
     */
    private BigDecimal totalDistance;

    /**
     * Flag indicating whether this bike path is published and visible to all users.
     * When true, the path is public and can be updated by any user.
     * When false, the path is private and only visible/editable by its creator.
     */
    private Boolean published;

    /**
     * List of GPS coordinates that form the complete route of the bike path.
     * Points are ordered by sequential position to reconstruct the path geometry.
     */
    private List<BikePathPointResponse> bikePathPoints;

    /**
     * List of obstacles reported along this bike path.
     * Includes both active and inactive obstacles for historical tracking.
     */
    private List<ObstacleResponse> obstacles;

}