package it.polimi.se.bbp.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request for user login.
 * @param email user email for authentication
 * @param password user password
 */
public record UserLoginRequest(

        /*
         * User email for authentication.
         */
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        /*
         * User password.
         */
        @NotBlank(message = "Password is required")
        String password

) {}