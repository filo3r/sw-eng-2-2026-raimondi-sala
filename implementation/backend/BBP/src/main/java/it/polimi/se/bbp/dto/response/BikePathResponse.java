package it.polimi.se.bbp.dto.response;

import it.polimi.se.bbp.enums.BikePathStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Response for BikePath containing complete route details, status, score, and obstacles.
 * @param id unique identifier
 * @param version version for optimistic locking
 * @param createdById ID of the user who created this bike path
 * @param createdByUsername username of the user who created this bike path
 * @param createdAt creation timestamp
 * @param updatedById ID of the user who last updated this bike path (null if never updated)
 * @param updatedByUsername username of the user who last updated this bike path (null if never updated)
 * @param updatedAt last update timestamp (null if never updated)
 * @param origin formatted address of starting point
 * @param originLatitude latitude of origin point
 * @param originLongitude longitude of origin point
 * @param destination formatted address of destination point
 * @param destinationLatitude latitude of destination point
 * @param destinationLongitude longitude of destination point
 * @param description optional description or notes
 * @param score overall quality score (range: 0.0 to 5.0, computed from status and obstacles)
 * @param status current condition status
 * @param statusDescription human-readable status description
 * @param totalDistance total distance in kilometers
 * @param published visibility flag (true = public and editable by anyone, false = private)
 * @param bikePathPoints GPS coordinates forming the complete route (ordered by sequential position)
 * @param obstacles list of obstacles along the path (includes active and inactive for history)
 */
public record BikePathResponse(

        /*
         * Unique identifier of the bike path.
         */
        Long id,

        /*
         * Version for optimistic locking.
         */
        Long version,

        /*
         * ID of the user who created this bike path.
         */
        Long createdById,

        /*
         * Username of the user who created this bike path.
         */
        String createdByUsername,

        /*
         * Creation timestamp.
         */
        OffsetDateTime createdAt,

        /*
         * ID of the user who last updated this bike path.
         * Null if never updated.
         */
        Long updatedById,

        /*
         * Username of the user who last updated this bike path.
         * Null if never updated.
         */
        String updatedByUsername,

        /*
         * Last update timestamp.
         * Null if never updated.
         */
        OffsetDateTime updatedAt,

        /*
         * Formatted address of the starting point.
         */
        String origin,

        /*
         * Latitude of the origin point.
         */
        Double originLatitude,

        /*
         * Longitude of the origin point.
         */
        Double originLongitude,

        /*
         * Formatted address of the destination point.
         */
        String destination,

        /*
         * Latitude of the destination point.
         */
        Double destinationLatitude,

        /*
         * Longitude of the destination point.
         */
        Double destinationLongitude,

        /*
         * Optional description or notes about the bike path.
         */
        String description,

        /*
         * Overall quality score of the bike path.
         * Range: 0.0 to 5.0
         * Computed from status, obstacles, and other factors.
         */
        BigDecimal score,

        /*
         * Current condition status.
         */
        BikePathStatus status,

        /*
         * Human-readable status description.
         * Example: "Excellent", "Good", "Under Maintenance"
         */
        String statusDescription,

        /*
         * Total distance in kilometers.
         */
        BigDecimal totalDistance,

        /*
         * Visibility flag.
         * True = public and editable by anyone.
         * False = private and only visible/editable by creator.
         */
        Boolean published,

        /*
         * GPS coordinates forming the complete route.
         * Ordered by sequential position.
         */
        List<BikePathPointResponse> bikePathPoints,

        /*
         * Obstacles reported along this bike path.
         * Includes active and inactive for historical tracking.
         */
        List<ObstacleResponse> obstacles

) {}