package it.polimi.se.bbp.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request for user registration.
 * @param name user's first name (max 50 chars)
 * @param surname user's last name (max 50 chars)
 * @param username unique username (max 50 chars)
 * @param email user's email address (max 150 chars)
 * @param password user's password (min 8 chars)
 */
public record UserRegisterRequest(

        /*
         * User's first name.
         * Maximum 50 characters.
         */
        @NotBlank(message = "Name is required")
        @Size(max = 50, message = "Name must not exceed 50 characters")
        String name,

        /*
         * User's last name.
         * Maximum 50 characters.
         */
        @NotBlank(message = "Surname is required")
        @Size(max = 50, message = "Surname must not exceed 50 characters")
        String surname,

        /*
         * Unique username.
         * Maximum 50 characters.
         */
        @NotBlank(message = "Username is required")
        @Size(max = 50, message = "Username must not exceed 50 characters")
        String username,

        /*
         * User's email address.
         * Maximum 150 characters.
         */
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 150, message = "Email must not exceed 150 characters")
        String email,

        /*
         * User's password.
         * Minimum 8 characters.
         */
        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password

) {}