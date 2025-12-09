package it.polimi.se.bbp.dto.response;

/**
 * Response for user information without sensitive data.
 * Excludes password and other sensitive fields.
 * @param id user ID
 * @param name user's first name
 * @param surname user's last name
 * @param username unique username
 * @param email user's email address
 */
public record UserResponse(

        /*
         * User ID.
         */
        Long id,

        /*
         * User's first name.
         */
        String name,

        /*
         * User's last name.
         */
        String surname,

        /*
         * Unique username.
         */
        String username,

        /*
         * User's email address.
         */
        String email

) {}