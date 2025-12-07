package it.polimi.se.bbp.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Request for user update with partial updates.
 * All fields are optional, only non-null fields will be updated.
 * @param name user's first name (optional, max 50 chars)
 * @param surname user's last name (optional, max 50 chars)
 * @param username unique username (optional, max 50 chars)
 * @param email user's email address (optional, max 150 chars)
 * @param password user's new password (optional, min 8 chars)
 */
public record UserUpdateRequest(

        /*
         * User's first name (optional).
         * Maximum 50 characters.
         */
        @Size(max = 50, message = "Name must not exceed 50 characters")
        String name,

        /*
         * User's last name (optional).
         * Maximum 50 characters.
         */
        @Size(max = 50, message = "Surname must not exceed 50 characters")
        String surname,

        /*
         * Unique username (optional).
         * Maximum 50 characters.
         */
        @Size(max = 50, message = "Username must not exceed 50 characters")
        String username,

        /*
         * User's email address (optional).
         * Maximum 150 characters.
         */
        @Email(message = "Email must be valid")
        @Size(max = 150, message = "Email must not exceed 150 characters")
        String email,

        /*
         * User's new password (optional).
         * Minimum 8 characters.
         */
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password

) {}