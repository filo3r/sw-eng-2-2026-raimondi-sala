package it.polimi.se.bbp.geo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Geographic coordinate with latitude and longitude.
 * Core domain class for location data handling.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Coordinate {

    /**
     * Latitude in decimal degrees.
     * Range: -90.0 to +90.0.
     */
    @NotNull
    @Min(value = -90, message = "Latitude must be >= -90")
    @Max(value = 90, message = "Latitude must be <= 90")
    private Double latitude;

    /**
     * Longitude in decimal degrees.
     * Range: -180.0 to +180.0.
     */
    @NotNull
    @Min(value = -180, message = "Longitude must be >= -180")
    @Max(value = 180, message = "Longitude must be <= 180")
    private Double longitude;

    /**
     * Static factory method to create Coordinate instance.
     * @param latitude latitude in decimal degrees
     * @param longitude longitude in decimal degrees
     * @return new Coordinate instance
     */
    public static Coordinate toCoordinate(Double latitude, Double longitude) {
        return new Coordinate(latitude, longitude);
    }

    /**
     * Converts to JTS Coordinate for geometry operations.
     * Note: JTS uses (x, y) order which corresponds to (longitude, latitude).
     * @return org.locationtech.jts.geom.Coordinate instance
     */
    public org.locationtech.jts.geom.Coordinate toJtsCoordinate() {
        return new org.locationtech.jts.geom.Coordinate(this.longitude, this.latitude);
    }

}