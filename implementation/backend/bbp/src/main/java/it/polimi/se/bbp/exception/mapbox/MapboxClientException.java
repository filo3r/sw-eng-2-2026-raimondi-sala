package it.polimi.se.bbp.exception.mapbox;

/**
 * Base exception for client-side errors during Mapbox API operations.
 * Indicates error in user's input data, not a technical problem.
 * Examples: invalid address, no route found, invalid coordinates.
 * Should result in HTTP 400 Bad Request.
 */
public abstract class MapboxClientException extends MapboxException {

    /**
     * Constructs Mapbox client exception with detail message.
     * @param message detail message explaining the error
     */
    public MapboxClientException(String message) {
        super(message);
    }

    /**
     * Constructs Mapbox client exception with detail message and cause.
     * @param message detail message explaining the error
     * @param cause underlying cause of this exception
     */
    public MapboxClientException(String message, Throwable cause) {
        super(message, cause);
    }

}