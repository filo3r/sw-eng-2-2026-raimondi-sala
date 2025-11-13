package it.polimi.se.bbp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * DTO for TripPoint response.
 * Contains GPS coordinate information for a single point in a trip route.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TripPointResponse {

    /**
     * Latitude coordinate in decimal degrees.
     */
    private Double latitude;

    /**
     * Longitude coordinate in decimal degrees.
     */
    private Double longitude;

    /**
     * Timestamp when this point was recorded.
     * Will be null for manually recorded trips.
     */
    private OffsetDateTime timestamp;

    /**
     * Sequential position of this point in the route (1-indexed).
     */
    private Integer sequentialPosition;

}