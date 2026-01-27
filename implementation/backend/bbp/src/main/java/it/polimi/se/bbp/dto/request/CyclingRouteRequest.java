package it.polimi.se.bbp.dto.request;

import it.polimi.se.bbp.geo.Coordinate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request for calculating a cycling route between waypoints.
 * @param waypoints ordered list of coordinates defining the route
 */
public record CyclingRouteRequest(

        /*
         * Ordered list of waypoints for the route.
         * Minimum 2 waypoints required.
         */
        @NotNull(message = "Waypoints cannot be null")
        @Size(min = 2, message = "At least 2 waypoints are required")
        @Valid
        List<Coordinate> waypoints

) {}