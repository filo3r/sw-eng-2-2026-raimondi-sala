package it.polimi.se.bbp.dto.request;

import it.polimi.se.bbp.enums.ObstacleSeverity;
import it.polimi.se.bbp.enums.ObstacleType;
import jakarta.validation.constraints.NotNull;

/**
 * Request for updating an existing obstacle with partial updates.
 * Only non-null fields will be updated.
 * Obstacle must belong to the bike path being updated.
 * @param id unique identifier of the obstacle to update
 * @param type new type or category (optional, e.g., POTHOLE, DEBRIS, CONSTRUCTION)
 * @param severity new severity level (optional, e.g., LOW, MEDIUM, HIGH, CRITICAL)
 * @param active active status (optional, false = resolved but kept for history)
 */
public record ObstacleUpdateRequest(

        /*
         * Unique identifier of the obstacle to update.
         * Must belong to the bike path being updated.
         */
        @NotNull(message = "Obstacle ID is required")
        Long id,

        /*
         * New type or category of the obstacle (optional).
         * Examples: POTHOLE, DEBRIS, CONSTRUCTION.
         */
        ObstacleType type,

        /*
         * New severity level of the obstacle (optional).
         * Possible values: LOW, MEDIUM, HIGH, CRITICAL.
         */
        ObstacleSeverity severity,

        /*
         * Active status (optional).
         * False = resolved or no longer present, kept for historical tracking.
         */
        Boolean active

        // Note: The location/address of an obstacle cannot be updated.
        // To change an obstacle's location, mark it inactive and create a new one.

) {}