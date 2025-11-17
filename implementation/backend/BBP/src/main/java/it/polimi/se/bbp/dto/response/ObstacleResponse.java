package it.polimi.se.bbp.dto.response;

import it.polimi.se.bbp.enums.ObstacleSeverity;
import it.polimi.se.bbp.enums.ObstacleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * DTO for Obstacle response.
 * Contains information about an obstacle reported on a bike path,
 * including its location, type, severity, and status.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ObstacleResponse {

    /**
     * The unique identifier of the obstacle.
     */
    private Long id;

    /**
     *
     */
    private Long version;

    /**
     * The ID of the user who originally reported or created this obstacle.
     * Will be null if the user who created it has been deleted from the system.
     */
    private Long createdBy;

    /**
     * The date and time when this obstacle was first reported.
     */
    private OffsetDateTime createdAt;

    /**
     * The ID of the user who last updated this obstacle.
     * Will be null if the obstacle has never been updated or if the user who updated it has been deleted.
     */
    private Long updatedBy;

    /**
     * The date and time of the last update to this obstacle.
     * Will be null if the obstacle has never been updated.
     */
    private OffsetDateTime updatedAt;

    /**
     * The formatted address or description of the location where the obstacle is present.
     * This is the geocoded address returned by the mapping service.
     */
    private String address;

    /**
     * Latitude coordinate of the obstacle's location in decimal degrees.
     * Valid range: -90.0 to +90.0
     */
    private Double latitude;

    /**
     * Longitude coordinate of the obstacle's location in decimal degrees.
     * Valid range: -180.0 to +180.0
     */
    private Double longitude;

    /**
     * The type or category of the obstacle.
     * Enum representing the nature of the problem.
     */
    private ObstacleType type;

    /**
     * The human-readable description of the obstacle type.
     * Example: "Pothole", "Debris", "Construction"
     */
    private String typeDescription;

    /**
     * The severity level of the obstacle.
     * Enum indicating how serious the problem is.
     */
    private ObstacleSeverity severity;

    /**
     * The human-readable description of the severity level.
     * Example: "Low", "Medium", "High", "Critical"
     */
    private String severityDescription;

    /**
     * Flag indicating whether this obstacle is currently active and present.
     * When false, the obstacle has been resolved or is no longer present,
     * but remains in the database for historical tracking.
     */
    private Boolean active;

}