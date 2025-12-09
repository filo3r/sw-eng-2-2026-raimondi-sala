package it.polimi.se.bbp.exception.openmeteo;

/**
 * Thrown when Open-Meteo API request times out or network connection fails.
 * Indicates temporary network issues or server unavailability.
 * Should result in HTTP 503 Service Unavailable.
 */
public class OpenMeteoTimeoutException extends OpenMeteoServiceException {

    /**
     * Constructs timeout exception with detail message.
     * @param message detail message explaining the timeout error
     */
    public OpenMeteoTimeoutException(String message) {
        super(message);
    }

    /**
     * Constructs timeout exception with detail message and cause.
     * @param message detail message explaining the timeout error
     * @param cause underlying cause of this exception
     */
    public OpenMeteoTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

}