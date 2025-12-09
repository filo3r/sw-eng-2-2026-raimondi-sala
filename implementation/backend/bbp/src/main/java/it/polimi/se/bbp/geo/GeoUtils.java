package it.polimi.se.bbp.geo;

/**
 * Utility class for geographic calculations and conversions.
 * Provides distance calculations (Haversine), bounding box generation,
 * and unit conversions. Stateless and non-instantiable.
 */
public final class GeoUtils {

    /**
     * Earth radius in kilometers (mean radius).
     */
    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * Approximate kilometers per degree of latitude (constant globally).
     */
    private static final double KM_PER_DEGREE_LAT = 111.0;

    /**
     * Private constructor to prevent instantiation.
     * @throws UnsupportedOperationException if instantiation is attempted via reflection
     */
    private GeoUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Calculates great-circle distance using Haversine formula.
     * Accounts for Earth's curvature, suitable for most navigation purposes.
     * @param from starting coordinate
     * @param to destination coordinate
     * @return distance between the two points in kilometers
     */
    public static double haversineDistance(Coordinate from, Coordinate to) {
        // Convert degrees to radians
        double lat1Rad = Math.toRadians(from.getLatitude());
        double lon1Rad = Math.toRadians(from.getLongitude());
        double lat2Rad = Math.toRadians(to.getLatitude());
        double lon2Rad = Math.toRadians(to.getLongitude());
        // Calculate coordinate differences
        double dLat = lat2Rad - lat1Rad;
        double dLon = lon2Rad - lon1Rad;
        // Apply Haversine formula
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        // Convert angular distance to kilometers
        return EARTH_RADIUS_KM * c;
    }

    /**
     * Calculates rectangular bounding box centered on a coordinate.
     * Useful for filtering database queries before precise calculations.
     * @param center center coordinate of the bounding box
     * @param radiusKm radius from center to box boundaries in kilometers
     * @return BoundingBox containing min/max latitude and longitude
     */
    public static BoundingBox calculateBoundingBox(Coordinate center, double radiusKm) {
        double centerLat = center.getLatitude();
        double centerLon = center.getLongitude();
        // Convert radius to latitude degrees (constant conversion)
        double latDegrees = radiusKm / KM_PER_DEGREE_LAT;
        // Convert radius to longitude degrees (latitude-dependent due to meridian convergence)
        double lonDegrees = radiusKm / (KM_PER_DEGREE_LAT * Math.cos(Math.toRadians(centerLat)));
        // Create bounding box with calculated boundaries
        return new BoundingBox(
                centerLat - latDegrees, // minLat
                centerLat + latDegrees, // maxLat
                centerLon - lonDegrees, // minLon
                centerLon + lonDegrees  // maxLon
        );
    }

    /**
     * Converts meters to decimal degrees.
     * Returns larger degree value (latitude or longitude equivalent) to ensure
     * safe buffer size covering the requested distance.
     * @param distanceMeters distance to convert in meters
     * @param latitude reference latitude for the calculation
     * @return approximate distance in decimal degrees (safe upper bound)
     * @throws IllegalArgumentException if latitude is outside valid range (-90 to 90)
     */
    public static double metersToDegrees(double distanceMeters, double latitude) {
        if (latitude < -90 || latitude > 90)
            throw new IllegalArgumentException("Latitude must be between -90 and 90.");
        // Convert distance to kilometers
        double distanceKm = distanceMeters / 1000.0;
        // Calculate degrees for latitude (constant)
        double latDegrees = distanceKm / KM_PER_DEGREE_LAT;
        // Calculate degrees for longitude (latitude-dependent)
        double lonDegrees = distanceKm / (KM_PER_DEGREE_LAT * Math.cos(Math.toRadians(latitude)));
        // Return the maximum value to guarantee full coverage (conservative buffer)
        return Math.max(latDegrees, lonDegrees);
    }

}