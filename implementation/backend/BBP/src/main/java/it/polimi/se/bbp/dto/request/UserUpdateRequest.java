package it.polimi.se.bbp.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for user update request.
 * All fields are optional to support partial updates (PATCH).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserUpdateRequest {

    /**
     * User's first name (optional).
     */
    @Size(max = 50, message = "Name must not exceed 50 characters")
    private String name;

    /**
     * User's last name (optional).
     */
    @Size(max = 50, message = "Surname must not exceed 50 characters")
    private String surname;

    /**
     * Unique username (optional).
     */
    @Size(max = 50, message = "Username must not exceed 50 characters")
    private String username;

    /**
     * User's email address (optional).
     */
    @Email(message = "Email must be valid")
    private String email;

    /**
     * User's new password (optional).
     */
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

}