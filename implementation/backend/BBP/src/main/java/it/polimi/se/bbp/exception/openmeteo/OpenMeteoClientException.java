package it.polimi.se.bbp.exception.openmeteo;

/**
 * Base exception for client errors when requesting weather data.
 * Indicates issues with user input or request parameters.
 * Should result in HTTP 400 Bad Request.
 */
public abstract class OpenMeteoClientException extends OpenMeteoException {

    /**
     * Constructs Open-Meteo client exception with detail message.
     * @param message detail message explaining the error
     */
    public OpenMeteoClientException(String message) {
        super(message);
    }

    /**
     * Constructs Open-Meteo client exception with detail message and cause.
     * @param message detail message explaining the error
     * @param cause underlying cause of this exception
     */
    public OpenMeteoClientException(String message, Throwable cause) {
        super(message, cause);
    }

}