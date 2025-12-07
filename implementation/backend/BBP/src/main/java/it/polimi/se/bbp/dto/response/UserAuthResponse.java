package it.polimi.se.bbp.dto.response;

/**
 * Response for user authentication containing JWT token and user ID.
 * @param token JWT access token for subsequent authenticated requests
 * @param userId user ID
 */
public record UserAuthResponse(

        /*
         * JWT access token.
         */
        String token,

        /*
         * User ID.
         */
        Long userId

) {}