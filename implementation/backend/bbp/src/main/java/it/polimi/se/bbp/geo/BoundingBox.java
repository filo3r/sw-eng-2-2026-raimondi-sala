package it.polimi.se.bbp.geo;

/**
 * Geographic rectangular bounding box.
 * Defines area using minimum and maximum latitude and longitude.
 * @param minLat minimum latitude (southern boundary)
 * @param maxLat maximum latitude (northern boundary)
 * @param minLon minimum longitude (western boundary)
 * @param maxLon maximum longitude (eastern boundary)
 */
public record BoundingBox(

        /* Minimum latitude in decimal degrees (southern boundary). */
        double minLat,

        /* Maximum latitude in decimal degrees (northern boundary). */
        double maxLat,

        /* Minimum longitude in decimal degrees (western boundary). */
        double minLon,

        /* Maximum longitude in decimal degrees (eastern boundary). */
        double maxLon

) {

    /**
     * Returns bottom-left coordinate of the bounding box.
     * @return Coordinate for min lat/lon
     */
    public Coordinate getMinCoordinate() {
        return Coordinate.toCoordinate(minLat, minLon);
    }

    /**
     * Returns top-right coordinate of the bounding box.
     * @return Coordinate for max lat/lon
     */
    public Coordinate getMaxCoordinate() {
        return Coordinate.toCoordinate(maxLat, maxLon);
    }

    /**
     * Checks if coordinate is contained within this bounding box.
     * @param coordinate coordinate to check
     * @return true if coordinate is inside or on boundary
     */
    public boolean contains(Coordinate coordinate) {
        if (coordinate == null)
            return false;
        else
            return coordinate.getLatitude() >= minLat &&
                    coordinate.getLatitude() <= maxLat &&
                    coordinate.getLongitude() >= minLon &&
                    coordinate.getLongitude() <= maxLon;
    }

}