package it.polimi.se.bbp.mapper.response;

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
     * @return the authentication response DTO
     */
    public UserAuthResponse toResponse(User user, String token) {
        return UserAuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .build();
    }

}