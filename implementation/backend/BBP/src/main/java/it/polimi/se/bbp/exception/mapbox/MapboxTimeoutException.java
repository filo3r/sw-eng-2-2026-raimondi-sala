package it.polimi.se.bbp.exception.mapbox;

/**
 * Thrown when a Mapbox API request times out.
 * Indicates network congestion, slow API response, or connection issues.
 * Caller may retry with longer timeout.
 * Should result in HTTP 503 Service Unavailable or HTTP 504 Gateway Timeout.
 */
public class MapboxTimeoutException extends MapboxServiceException {

    /**
     * Constructs timeout exception with detail message.
     * @param message detail message explaining the timeout error
     */
    public MapboxTimeoutException(String message) {
        super(message);
    }

    /**
     * Constructs timeout exception with detail message and cause.
     * @param message detail message explaining the timeout error
     * @param cause underlying cause of this exception
     */
    public MapboxTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

}