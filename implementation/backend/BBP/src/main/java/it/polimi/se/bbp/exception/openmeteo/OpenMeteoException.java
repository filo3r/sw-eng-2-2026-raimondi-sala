package it.polimi.se.bbp.exception.openmeteo;

/**
 * Base exception for all Open-Meteo related errors.
 * Unchecked exception, allows unified exception handling.
 */
public abstract class OpenMeteoException extends RuntimeException {

    /**
     * Constructs Open-Meteo exception with detail message.
     * @param message detail message explaining the error
     */
    public OpenMeteoException(String message) {
        super(message);
    }

    /**
     * Constructs Open-Meteo exception with detail message and cause.
     * @param message detail message explaining the error
     * @param cause underlying cause of this exception
     */
    public OpenMeteoException(String message, Throwable cause) {
        super(message, cause);
    }

}