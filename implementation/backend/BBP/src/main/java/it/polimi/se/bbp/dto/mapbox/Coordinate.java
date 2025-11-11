package it.polimi.se.bbp.dto.mapbox;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a geographic coordinate with latitude and longitude.
 * This class is used throughout the application for location data.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Coordinate {

    /**
     * Latitude in decimal degrees.
     * Valid range: -90.0 to +90.0
     */
    private Double latitude;

    /**
     * Longitude in decimal degrees.
     * Valid range: -180.0 to +180.0
     */
    private Double longitude;

}