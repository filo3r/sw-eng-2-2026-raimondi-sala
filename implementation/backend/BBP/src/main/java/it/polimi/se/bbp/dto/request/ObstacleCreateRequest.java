package it.polimi.se.bbp.dto.request;

import it.polimi.se.bbp.enums.ObstacleSeverity;
import it.polimi.se.bbp.enums.ObstacleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a new obstacle on a bike path.
 * Contains the user-provided data needed to create an obstacle.
 * The address will be geocoded to obtain coordinates.
 * The 'active' field is always set to true when creating a new obstacle.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ObstacleCreateRequest {

    /**
     * The address or location description of the obstacle.
     * This will be geocoded to obtain latitude and longitude coordinates.
     * This field is required and cannot exceed 256 characters.
     */
    @NotBlank(message = "Obstacle address is required")
    @Size(max = 256, message = "Obstacle address must not exceed 256 characters")
    private String address;

    /**
     * The type or category of the obstacle.
     * This field is required and describes the nature of the problem.
     * Examples: POTHOLE, DEBRIS, CONSTRUCTION, etc.
     */
    @NotNull(message = "Obstacle type is required")
    private ObstacleType type;

    /**
     * The severity level of the obstacle.
     * This field is required and indicates how serious the problem is.
     * Possible values: LOW, MEDIUM, HIGH, CRITICAL.
     */
    @NotNull(message = "Obstacle severity is required")
    private ObstacleSeverity severity;

    // Note: 'active' field is not included in the request.
    // All newly created obstacles are automatically set to active=true.

}