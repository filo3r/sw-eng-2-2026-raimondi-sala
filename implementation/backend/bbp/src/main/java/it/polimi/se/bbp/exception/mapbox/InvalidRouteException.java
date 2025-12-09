package it.polimi.se.bbp.exception.mapbox;

/**
 * Thrown when a route cannot be calculated between provided waypoints.
 * Indicates user error: waypoints are unreachable or invalid for routing.
 * Possible reasons: disconnected locations, waypoints not connectable on cycling paths,
 * invalid coordinates, or inaccessible areas.
 * Should result in HTTP 400 Bad Request.
 */
public class InvalidRouteException extends MapboxClientException {

    /**
     * Constructs exception for invalid route.
     * @param message detailed error message describing the specific route failure
     */
    public InvalidRouteException(String message) {
        super(message);
    }

    /**
     * Constructs exception for invalid route with cause.
     * @param message detailed error message describing the specific route failure
     * @param cause underlying cause of this exception
     */
    public InvalidRouteException(String message, Throwable cause) {
        super(message, cause);
    }

}