package it.polimi.se.bbp.dto.request;

import it.polimi.se.bbp.enums.BikePathStatus;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for updating an existing bike path.
 * Supports partial updates - only non-null fields will be updated.
 * All fields are optional to support flexible update operations.
 * Permission rules:
 * - The creator can always update their bike path (public or private)
 * - Other users can only update published (public) bike paths
 * - Only the creator can delete a bike path
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BikePathUpdateRequest {

    /**
     *
     */
    private Long version;

    /**
     * The new condition status of the bike path (optional).
     * If provided, the bike path status will be updated to this value.
     * If null, the existing status will remain unchanged.
     * Possible values include: EXCELLENT, GOOD, SUFFICIENT, POOR, UNDER_MAINTENANCE, etc.
     * The status affects the bike path score calculation.
     */
    private BikePathStatus status;

    /**
     * Flag indicating whether this bike path should be published (optional).
     * If provided, the published status will be updated to this value.
     * If null, the existing published status will remain unchanged.
     * When true, the path becomes visible to all users and can be updated by anyone.
     * When false, the path is only visible to its creator and can only be updated by the creator.
     * Note: Changing from published=true to published=false does not revoke update permissions
     * from users who have already contributed updates while it was public.
     */
    private Boolean published;

    /**
     * List of new obstacles to add to the bike path (optional).
     * If provided, these obstacles will be created and associated with the bike path.
     * Each obstacle contains an address (to be geocoded), type, and severity.
     * All new obstacles will be validated to ensure they are within a reasonable distance
     * from the bike path route.
     * If null or empty, no obstacles will be added.
     */
    @Valid
    private List<@Valid ObstacleCreateRequest> obstaclesToAdd;

    /**
     * List of existing obstacles to update (optional).
     * If provided, the specified obstacles will be modified with the new values.
     * Each update request must include the obstacle ID to identify which obstacle to modify.
     * Only the non-null fields in each update request will be changed (partial update).
     * The obstacles being updated must belong to this bike path.
     * If null or empty, no existing obstacles will be updated.
     */
    @Valid
    private List<@Valid ObstacleUpdateRequest> obstaclesToUpdate;

    // Note: The bike path route (addresses, waypoints) cannot be updated.
    // To change the route, create a new bike path.
    // The description field is also not updateable in this version.

}