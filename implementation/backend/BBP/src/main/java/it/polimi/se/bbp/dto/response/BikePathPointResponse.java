package it.polimi.se.bbp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * DTO for BikePathPoint response.
 * Contains GPS coordinate information for a single point in a bike path route.
 * These points define the complete geometry of the bike path.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BikePathPointResponse {

    /**
     * Latitude coordinate in decimal degrees.
     * Valid range: -90.0 to +90.0
     */
    private Double latitude;

    /**
     * Longitude coordinate in decimal degrees.
     * Valid range: -180.0 to +180.0
     */
    private Double longitude;

    /**
     * Timestamp when this point was recorded.
     * Will be null for manually created bike paths.
     * For GPS-tracked bike paths, this contains the exact time when the cyclist
     * passed through this coordinate.
     */
    private OffsetDateTime timestamp;

    /**
     * Sequential position of this point in the route (1-indexed).
     * Lower values indicate earlier points in the path.
     * This ordering allows reconstruction of the complete route in the correct sequence.
     */
    private Integer sequentialPosition;

}