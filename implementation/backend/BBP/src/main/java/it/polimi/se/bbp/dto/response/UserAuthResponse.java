package it.polimi.se.bbp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for authentication response.
 * Contains the JWT token and user information.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserAuthResponse {

    /**
     * JWT access token.
     */
    private String token;

    /**
     * User ID.
     */
    private Long userId;

    /**
     * Username.
     */
    private String username;

    /**
     * User's email.
     */
    private String email;

    /**
     * User's first name.
     */
    private String name;

    /**
     * User's last name.
     */
    private String surname;

    /**
     * Message about the authentication result.
     */
    private String message;

}