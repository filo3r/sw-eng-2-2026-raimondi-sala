package it.polimi.se.bbp.dto.request;

import it.polimi.se.bbp.enums.ObstacleSeverity;
import it.polimi.se.bbp.enums.ObstacleType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating an existing obstacle on a bike path.
 * Supports partial updates - only non-null fields will be updated.
 * The obstacle is identified by its ID, and it must belong to the bike path being updated.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ObstacleUpdateRequest {

    /**
     * The unique identifier of the obstacle to update.
     * This field is required and is used to identify which obstacle should be modified.
     * The obstacle must belong to the bike path being updated.
     */
    @NotNull(message = "Obstacle ID is required")
    private Long id;

    /**
     *
     */
    private Long version;

    /**
     * The new type or category of the obstacle (optional).
     * If provided, the obstacle type will be updated to this value.
     * If null, the existing type will remain unchanged.
     * Examples: POTHOLE, DEBRIS, CONSTRUCTION, etc.
     */
    private ObstacleType type;

    /**
     * The new severity level of the obstacle (optional).
     * If provided, the obstacle severity will be updated to this value.
     * If null, the existing severity will remain unchanged.
     * Possible values: LOW, MEDIUM, HIGH, CRITICAL.
     */
    private ObstacleSeverity severity;

    /**
     * Flag indicating whether this obstacle is currently active (optional).
     * If provided, the active status will be updated to this value.
     * If null, the existing active status will remain unchanged.
     * When set to false, the obstacle is considered resolved or no longer present,
     * but it remains in the database for historical tracking.
     */
    private Boolean active;

    // Note: The location/address of an obstacle cannot be updated.
    // To change an obstacle's location, it should be marked as inactive and a new one created.

}