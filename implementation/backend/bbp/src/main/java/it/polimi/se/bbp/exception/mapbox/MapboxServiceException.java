package it.polimi.se.bbp.exception.mapbox;

/**
 * Base exception for technical/service errors during Mapbox API operations.
 * Indicates server-side error (5xx) or infrastructure problem, not user input error.
 * Examples: network timeouts, HTTP 5xx errors, rate limiting, malformed responses.
 * Should result in HTTP 503 Service Unavailable or HTTP 429 Too Many Requests.
 */
public abstract class MapboxServiceException extends MapboxException {

    /**
     * Constructs Mapbox service exception with detail message.
     * @param message detail message explaining the error
     */
    public MapboxServiceException(String message) {
        super(message);
    }

    /**
     * Constructs Mapbox service exception with detail message and cause.
     * @param message detail message explaining the error
     * @param cause underlying cause of this exception
     */
    public MapboxServiceException(String message, Throwable cause) {
        super(message, cause);
    }

}