package it.polimi.se.bbp.dto.response;

/**
 * Response for a single point in a calculated route.
 * @param latitude latitude in decimal degrees
 * @param longitude longitude in decimal degrees
 * @param sequentialPosition sequential position in route (0-indexed)
 */
public record CyclingRoutePointResponse(

        /*
         * Latitude in decimal degrees.
         */
        Double latitude,

        /*
         * Longitude in decimal degrees.
         */
        Double longitude,

        /*
         * Sequential position in the route.
         * 1-indexed order of this point in the complete route.
         */
        Integer sequentialPosition

) {}