package it.polimi.se.bbp.exception.mapbox;

/**
 * Thrown when Mapbox API rate limit is exceeded.
 * Occurs when too many requests are made in a short time period.
 * Caller should implement exponential backoff or retry after delay.
 * Should result in HTTP 429 Too Many Requests with Retry-After header.
 */
public class MapboxRateLimitException extends MapboxServiceException {

    /**
     * Constructs rate limit exception with detail message.
     * @param message detail message explaining the rate limit error
     */
    public MapboxRateLimitException(String message) {
        super(message);
    }

    /**
     * Constructs rate limit exception with detail message and cause.
     * @param message detail message explaining the rate limit error
     * @param cause underlying cause of this exception
     */
    public MapboxRateLimitException(String message, Throwable cause) {
        super(message, cause);
    }

}