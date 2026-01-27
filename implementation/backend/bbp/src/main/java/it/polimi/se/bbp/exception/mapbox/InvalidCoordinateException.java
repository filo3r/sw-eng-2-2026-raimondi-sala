package it.polimi.se.bbp.exception.mapbox;

import it.polimi.se.bbp.geo.Coordinate;
import lombok.Getter;

/**
 * Thrown when coordinates cannot be reverse geocoded because no address is found.
 * Indicates user error: no address found for provided coordinates.
 * Should result in HTTP 400 Bad Request.
 */
@Getter
public class InvalidCoordinateException extends MapboxClientException {

    /**
     * Coordinate that could not be reverse geocoded.
     */
    private final Coordinate coordinate;

    /**
     * Constructs exception for invalid coordinate.
     * @param coordinate coordinate that could not be reverse geocoded
     */
    public InvalidCoordinateException(Coordinate coordinate) {
        super(String.format("Unable to reverse geocode coordinates: (%.6f, %.6f). No address found at this location.",
                coordinate.getLatitude(), coordinate.getLongitude()));
        this.coordinate = coordinate;
    }

    /**
     * Constructs exception for invalid coordinate with cause.
     * @param coordinate coordinate that could not be reverse geocoded
     * @param cause underlying cause of this exception
     */
    public InvalidCoordinateException(Coordinate coordinate, Throwable cause) {
        super(String.format("Unable to reverse geocode coordinates: (%.6f, %.6f). No address found at this location.",
                coordinate.getLatitude(), coordinate.getLongitude()), cause);
        this.coordinate = coordinate;
    }

}