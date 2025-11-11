package it.polimi.se.bbp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for user response.
 * Contains user information without sensitive data like password.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserResponse {

    /**
     * User ID.
     */
    private Long id;

    /**
     * User's first name.
     */
    private String name;

    /**
     * User's last name.
     */
    private String surname;

    /**
     * Unique username.
     */
    private String username;

    /**
     * User's email address.
     */
    private String email;

}