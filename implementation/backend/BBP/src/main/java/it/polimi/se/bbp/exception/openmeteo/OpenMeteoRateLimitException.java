package it.polimi.se.bbp.exception.openmeteo;

/**
 * Thrown when Open-Meteo API rate limit is exceeded.
 * Occurs from internal rate limiter or API's 429 Too Many Requests response.
 * Should result in HTTP 429 Too Many Requests.
 */
public class OpenMeteoRateLimitException extends OpenMeteoServiceException {

    /**
     * Constructs rate limit exception with detail message.
     * @param message detail message explaining the rate limit error
     */
    public OpenMeteoRateLimitException(String message) {
        super(message);
    }

    /**
     * Constructs rate limit exception with detail message and cause.
     * @param message detail message explaining the rate limit error
     * @param cause underlying cause of this exception
     */
    public OpenMeteoRateLimitException(String message, Throwable cause) {
        super(message, cause);
    }

}