package it.polimi.se.bbp.exception.obstacle;

/**
 * Base exception for all obstacle-related errors.
 * Occurs during obstacle operations such as creation, validation, or update.
 * Unchecked exception, no explicit declaration required.
 */
public class ObstacleException extends RuntimeException {

    /**
     * Constructs obstacle exception with detail message.
     * @param message detail message explaining the reason for the exception
     */
    public ObstacleException(String message) {
        super(message);
    }

    /**
     * Constructs obstacle exception with detail message and cause.
     * @param message detail message explaining the reason for the exception
     * @param cause underlying cause of this exception
     */
    public ObstacleException(String message, Throwable cause) {
        super(message, cause);
    }

}