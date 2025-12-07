package it.polimi.se.bbp.dto.request;

import it.polimi.se.bbp.enums.BikePathStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request for updating an existing bike path with partial updates.
 * Only non-null fields will be updated, supporting flexible modifications.
 * Permission rules: creator can always update, other users can only update published paths.
 * @param version version for optimistic locking
 * @param status new condition status (optional, affects score calculation)
 * @param description new description (optional, empty string clears it, max 500 chars)
 * @param published visibility flag (optional, true = public, false = private)
 * @param obstaclesToAdd new obstacles to add (optional, will be geocoded and validated)
 * @param obstaclesToUpdate existing obstacles to modify (optional, partial updates by ID)
 */
public record BikePathUpdateRequest(

        /*
         * Version for optimistic locking.
         */
        Long version,

        /*
         * New condition status of the bike path (optional).
         * Affects score calculation.
         */
        BikePathStatus status,

        /*
         * New description for the bike path (optional).
         * Empty string clears the description.
         * Maximum 500 characters.
         */
        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,

        /*
         * Visibility flag (optional).
         * True = visible to all and editable by anyone.
         * False = visible and editable only by creator.
         */
        Boolean published,

        /*
         * New obstacles to add to the bike path (optional).
         * Will be geocoded and validated against route distance.
         */
        @Valid
        List<@Valid ObstacleCreateRequest> obstaclesToAdd,

        /*
         * Existing obstacles to update (optional).
         * Identified by ID, supports partial updates.
         * Must belong to this bike path.
         */
        @Valid
        List<@Valid ObstacleUpdateRequest> obstaclesToUpdate

        // Note: The bike path route (addresses, waypoints) cannot be updated.
        // To change the route, create a new bike path.

) {}