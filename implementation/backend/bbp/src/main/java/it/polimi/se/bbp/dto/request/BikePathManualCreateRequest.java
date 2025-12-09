package it.polimi.se.bbp.dto.request;

import it.polimi.se.bbp.enums.BikePathStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request for manually creating a bike path by providing addresses.
 * Addresses will be geocoded and used to calculate the cycling route.
 * Supports optional obstacles that will be validated against the route.
 * @param addresses ordered list of addresses defining the bike path route (min 2: origin and destination)
 * @param description optional description or notes about the bike path (max 500 chars)
 * @param status current condition status affecting score calculation
 * @param published visibility flag (true = public and editable by anyone, false = private)
 * @param obstacles list of obstacles along the path (can be empty)
 */
public record BikePathManualCreateRequest(

        /*
         * Ordered list of addresses defining the bike path route.
         * First is origin, last is destination, intermediate are waypoints.
         * Minimum 2 addresses required.
         */
        @NotNull(message = "Addresses list is required")
        @NotEmpty(message = "At least 2 addresses are required (origin and destination)")
        @Size(min = 2, message = "At least 2 addresses are required (origin and destination)")
        List<@NotBlank(message = "Address cannot be blank") @Size(max = 256, message = "Address must not exceed 256 characters") String> addresses,

        /*
         * Optional description or notes about the bike path.
         * Maximum 500 characters.
         */
        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,

        /*
         * Current condition status of the bike path.
         * Affects score calculation.
         */
        @NotNull(message = "Status is required")
        BikePathStatus status,

        /*
         * Visibility flag.
         * True = visible to all and editable by anyone.
         * False = visible and editable only by creator.
         */
        @NotNull(message = "Published flag is required")
        Boolean published,

        /*
         * List of obstacles along the bike path.
         * Can be empty if no obstacles.
         * Validated to be within reasonable distance from route.
         */
        @NotNull(message = "Obstacles list is required (use empty list if no obstacles)")
        List<@Valid ObstacleCreateRequest> obstacles

) {}