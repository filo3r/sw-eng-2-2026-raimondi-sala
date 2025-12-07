package it.polimi.se.bbp.dto.response;

import it.polimi.se.bbp.enums.ObstacleSeverity;
import it.polimi.se.bbp.enums.ObstacleType;

import java.time.OffsetDateTime;

/**
 * Response for Obstacle containing location, type, severity, and status.
 * Includes both active and resolved obstacles for historical tracking.
 * @param id unique identifier
 * @param createdById ID of user who reported this obstacle (null if user deleted)
 * @param createdByUsername username of user who reported this obstacle (null if user deleted)
 * @param createdAt creation timestamp
 * @param updatedById ID of user who last updated this obstacle (null if never updated or user deleted)
 * @param updatedByUsername username of user who last updated this obstacle (null if never updated or user deleted)
 * @param updatedAt last update timestamp (null if never updated)
 * @param address formatted address of obstacle location (geocoded)
 * @param latitude latitude coordinate in decimal degrees (range: -90.0 to +90.0)
 * @param longitude longitude coordinate in decimal degrees (range: -180.0 to +180.0)
 * @param type type or category of the obstacle
 * @param typeDescription human-readable obstacle type (e.g., "Pothole", "Debris", "Construction")
 * @param severity severity level of the obstacle
 * @param severityDescription human-readable severity level (e.g., "Low", "Medium", "High", "Critical")
 * @param active flag indicating if obstacle is currently present (false = resolved but kept for history)
 * @param positionOnPath position of obstacle along the bike path route
 */
public record ObstacleResponse(

        /*
         * Unique identifier of the obstacle.
         */
        Long id,

        /*
         * ID of the user who reported this obstacle.
         * Null if user deleted.
         */
        Long createdById,

        /*
         * Username of the user who reported this obstacle.
         * Null if user deleted.
         */
        String createdByUsername,

        /*
         * Creation timestamp.
         */
        OffsetDateTime createdAt,

        /*
         * ID of the user who last updated this obstacle.
         * Null if never updated or user deleted.
         */
        Long updatedById,

        /*
         * Username of the user who last updated this obstacle.
         * Null if never updated or user deleted.
         */
        String updatedByUsername,

        /*
         * Last update timestamp.
         * Null if never updated.
         */
        OffsetDateTime updatedAt,

        /*
         * Formatted address of obstacle location.
         * Geocoded address from mapping service.
         */
        String address,

        /*
         * Latitude coordinate in decimal degrees.
         * Valid range: -90.0 to +90.0
         */
        Double latitude,

        /*
         * Longitude coordinate in decimal degrees.
         * Valid range: -180.0 to +180.0
         */
        Double longitude,

        /*
         * Type or category of the obstacle.
         */
        ObstacleType type,

        /*
         * Human-readable obstacle type description.
         * Example: "Pothole", "Debris", "Construction"
         */
        String typeDescription,

        /*
         * Severity level of the obstacle.
         */
        ObstacleSeverity severity,

        /*
         * Human-readable severity level description.
         * Example: "Low", "Medium", "High", "Critical"
         */
        String severityDescription,

        /*
         * Active status flag.
         * False = resolved or no longer present, kept for historical tracking.
         */
        Boolean active,

        /*
         * Position of obstacle along the bike path route.
         */
        Integer positionOnPath

) {}