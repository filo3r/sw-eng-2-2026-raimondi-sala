package it.polimi.se.bbp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for authentication response.
 * Contains the JWT token and user id.
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

}