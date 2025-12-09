package it.polimi.se.bbp.exception.mapbox;

/**
 * Base exception for all Mapbox API-related errors.
 * Unchecked exception as Mapbox errors are typically non-recoverable.
 * All Mapbox exceptions extend this class for unified exception handling.
 */
public abstract class MapboxException extends RuntimeException {

    /**
     * Constructs Mapbox exception with detail message.
     * @param message detail message explaining the error
     */
    public MapboxException(String message) {
        super(message);
    }

    /**
     * Constructs Mapbox exception with detail message and cause.
     * @param message detail message explaining the error
     * @param cause underlying cause of this exception
     */
    public MapboxException(String message, Throwable cause) {
        super(message, cause);
    }

}