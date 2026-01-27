package it.polimi.se.bbp.dto.response;

import java.util.List;

/**
 * Response for calculated cycling route.
 * @param points ordered list of route points with coordinates
 */
public record CyclingRouteResponse(

        /*
         * Ordered list of route points.
         * Points are ordered by sequentialPosition.
         */
        List<CyclingRoutePointResponse> points

) {}