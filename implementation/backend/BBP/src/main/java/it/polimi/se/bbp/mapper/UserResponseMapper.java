package it.polimi.se.bbp.mapper;

import it.polimi.se.bbp.dto.response.UserResponse;
import it.polimi.se.bbp.entity.User;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting User entities to UserResponse DTOs.
 */
@Component
public class UserResponseMapper {

    /**
     * Converts a User entity to a UserResponse DTO.
     * Excludes sensitive information like password.
     * @param user the user entity
     * @param message the response message
     * @return the user response DTO
     */
    public UserResponse toResponse(User user, String message) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .surname(user.getSurname())
                .username(user.getUsername())
                .email(user.getEmail())
                .message(message)
                .build();
    }

}