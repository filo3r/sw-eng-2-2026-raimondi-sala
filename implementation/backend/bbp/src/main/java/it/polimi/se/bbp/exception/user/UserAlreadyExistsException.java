package it.polimi.se.bbp.exception.user;

/**
 * Thrown when attempting to create a user that already exists.
 * Occurs when username or email is already registered in the system.
 * Should result in HTTP 409 Conflict.
 */
public class UserAlreadyExistsException extends RuntimeException {

    /**
     * Constructs exception for user already exists.
     * @param message detail message explaining which field is duplicated
     */
    public UserAlreadyExistsException(String message) {
        super(message);
    }

}