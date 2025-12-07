package it.polimi.se.bbp.dto.result;

import it.polimi.se.bbp.geo.Coordinate;

import java.util.List;

/**
 * Result of a cycling route calculation containing geometry and distance.
 * @param routeCoordinates ordered list of coordinates forming the complete route
 * @param distanceInMeters total route distance in meters
 */
public record CyclingRouteResult(

        /*
         * Ordered list of coordinates forming the complete route.
         */
        List<Coordinate> routeCoordinates,

        /*
         * Total route distance in meters.
         */
        Double distanceInMeters

) {}