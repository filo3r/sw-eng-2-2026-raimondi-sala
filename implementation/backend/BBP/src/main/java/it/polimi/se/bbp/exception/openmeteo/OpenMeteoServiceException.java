package it.polimi.se.bbp.exception.openmeteo;

/**
 * Base exception for technical errors when interacting with Open-Meteo API.
 * Indicates issues with weather service, not user input.
 * Should result in HTTP 503 Service Unavailable or HTTP 429 Too Many Requests.
 */
public abstract class OpenMeteoServiceException extends OpenMeteoException {

    /**
     * Constructs Open-Meteo service exception with detail message.
     * @param message detail message explaining the error
     */
    public OpenMeteoServiceException(String message) {
        super(message);
    }

    /**
     * Constructs Open-Meteo service exception with detail message and cause.
     * @param message detail message explaining the error
     * @param cause underlying cause of this exception
     */
    public OpenMeteoServiceException(String message, Throwable cause) {
        super(message, cause);
    }

}