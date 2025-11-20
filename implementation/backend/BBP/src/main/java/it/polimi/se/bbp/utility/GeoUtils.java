package it.polimi.se.bbp.utility;

import lombok.Builder;
import lombok.Data;

/**
 * Utility class for geographic distance calculations.
 * Provides Haversine distance and bounding box calculations for location-based searches.
 */
public class GeoUtils {

    /**
     * Earth radius in kilometers (mean radius).
     */
    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * Kilometers per degree of latitude (constant globally).
     */
    private static final double KM_PER_DEGREE_LAT = 111.0;

    /**
     * Private constructor to prevent instantiation.
     */
    private GeoUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Calculates distance between two points using Haversine formula.
     * Accounts for Earth's curvature and provides accurate distance calculations.
     * @param lat1 latitude of first point in degrees
     * @param lon1 longitude of first point in degrees
     * @param lat2 latitude of second point in degrees
     * @param lon2 longitude of second point in degrees
     * @return distance in kilometers
     */
    public static double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        // Convert degrees to radians
        double lat1Rad = Math.toRadians(lat1);
        double lon1Rad = Math.toRadians(lon1);
        double lat2Rad = Math.toRadians(lat2);
        double lon2Rad = Math.toRadians(lon2);
        // Differences
        double dLat = lat2Rad - lat1Rad;
        double dLon = lon2Rad - lon1Rad;
        // Haversine formula
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        // Distance in kilometers
        return EARTH_RADIUS_KM * c;
    }

    /**
     * Calculates a rectangular bounding box around a point for given radius.
     * Used for fast database filtering before precise Haversine calculations.
     * @param latitude center latitude in degrees
     * @param longitude center longitude in degrees
     * @param radiusKm radius in kilometers
     * @return bounding box with min/max lat/lon
     */
    public static BoundingBox calculateBoundingBox(double latitude, double longitude, double radiusKm) {
        // Latitude degrees per km (constant)
        double latDegreesPerKm = 1.0 / KM_PER_DEGREE_LAT;
        // Longitude degrees per km (varies with latitude)
        double lonDegreesPerKm = 1.0 / (KM_PER_DEGREE_LAT * Math.cos(Math.toRadians(latitude)));
        // Calculate deltas
        double deltaLat = radiusKm * latDegreesPerKm;
        double deltaLon = radiusKm * lonDegreesPerKm;
        return BoundingBox.builder()
                .minLat(latitude - deltaLat)
                .maxLat(latitude + deltaLat)
                .minLon(longitude - deltaLon)
                .maxLon(longitude + deltaLon)
                .build();
    }

    /**
     * Data class representing a geographic bounding box.
     * Defines a rectangular area with minimum and maximum latitude/longitude.
     */
    @Data
    @Builder
    public static class BoundingBox {
        private double minLat;
        private double maxLat;
        private double minLon;
        private double maxLon;
    }

}