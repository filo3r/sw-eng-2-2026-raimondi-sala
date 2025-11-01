package it.polimi.se.bbp.mapper;

import it.polimi.se.bbp.dto.response.UserAuthResponse;
import it.polimi.se.bbp.entity.User;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting User entities to authentication response DTOs.
 */
@Component
public class UserAuthResponseMapper {

    /**
     * Converts a User entity to a UserAuthResponse DTO.
     * @param user the user entity
     * @param token the JWT token
     * @param message the response message
     * @return the authentication response DTO
     */
    public UserAuthResponse toAuthResponse(User user, String token, String message) {
        return UserAuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .name(user.getName())
                .surname(user.getSurname())
                .message(message)
                .build();
    }

}