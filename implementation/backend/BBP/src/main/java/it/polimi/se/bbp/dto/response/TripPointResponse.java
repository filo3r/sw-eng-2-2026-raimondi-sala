package it.polimi.se.bbp.dto.response;

import java.time.OffsetDateTime;

/**
 * Response for a single TripPoint containing GPS coordinates.
 * Points define the complete geometry of the trip route.
 * @param latitude latitude coordinate in decimal degrees
 * @param longitude longitude coordinate in decimal degrees
 * @param timestamp timestamp when point was recorded (null for manually recorded trips)
 * @param sequentialPosition sequential position in the route (1-indexed, for ordering)
 */
public record TripPointResponse(

        /*
         * Latitude coordinate in decimal degrees.
         */
        Double latitude,

        /*
         * Longitude coordinate in decimal degrees.
         */
        Double longitude,

        /*
         * Timestamp when this point was recorded.
         * Null for manually recorded trips.
         */
        OffsetDateTime timestamp,

        /*
         * Sequential position of this point in the route (1-indexed).
         */
        Integer sequentialPosition

) {}