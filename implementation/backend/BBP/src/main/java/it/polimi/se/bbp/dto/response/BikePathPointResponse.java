package it.polimi.se.bbp.dto.response;

import java.time.OffsetDateTime;

/**
 * Response for a single BikePathPoint containing GPS coordinates.
 * Points define the complete geometry of the bike path route.
 * @param latitude latitude coordinate in decimal degrees (range: -90.0 to +90.0)
 * @param longitude longitude coordinate in decimal degrees (range: -180.0 to +180.0)
 * @param timestamp timestamp when point was recorded (null for manually created paths)
 * @param sequentialPosition sequential position in the route (1-indexed, for ordering)
 */
public record BikePathPointResponse(

        /*
         * Latitude coordinate in decimal degrees.
         * Valid range: -90.0 to +90.0
         */
        Double latitude,

        /*
         * Longitude coordinate in decimal degrees.
         * Valid range: -180.0 to +180.0
         */
        Double longitude,

        /*
         * Timestamp when this point was recorded.
         * Null for manually created bike paths.
         * For GPS-tracked paths, exact time when cyclist passed through this coordinate.
         */
        OffsetDateTime timestamp,

        /*
         * Sequential position of this point in the route (1-indexed).
         * Lower values indicate earlier points in the path.
         */
        Integer sequentialPosition

) {}