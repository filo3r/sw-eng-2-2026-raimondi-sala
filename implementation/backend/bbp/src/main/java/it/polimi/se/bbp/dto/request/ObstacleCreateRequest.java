package it.polimi.se.bbp.dto.request;

import it.polimi.se.bbp.enums.ObstacleSeverity;
import it.polimi.se.bbp.enums.ObstacleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request for creating a new obstacle on a bike path.
 * Address will be geocoded to obtain coordinates.
 * All newly created obstacles are automatically set to active=true.
 * @param address address or location description (will be geocoded, max 256 chars)
 * @param type type or category of the obstacle
 * @param severity severity level of the obstacle
 */
public record ObstacleCreateRequest(

        /*
         * Address or location description of the obstacle.
         * Will be geocoded to obtain coordinates.
         * Maximum 256 characters.
         */
        @NotBlank(message = "Obstacle address is required")
        @Size(max = 256, message = "Obstacle address must not exceed 256 characters")
        String address,

        /*
         * Type or category of the obstacle.
         * Examples: POTHOLE, DEBRIS, CONSTRUCTION.
         */
        @NotNull(message = "Obstacle type is required")
        ObstacleType type,

        /*
         * Severity level of the obstacle.
         * Possible values: LOW, MEDIUM, HIGH, CRITICAL.
         */
        @NotNull(message = "Obstacle severity is required")
        ObstacleSeverity severity

        // Note: All newly created obstacles are automatically set to active=true.

) {}