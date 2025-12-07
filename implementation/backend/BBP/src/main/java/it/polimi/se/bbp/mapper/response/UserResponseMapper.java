package it.polimi.se.bbp.mapper.response;

import it.polimi.se.bbp.dto.response.UserResponse;
import it.polimi.se.bbp.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * Mapper for converting User entities to UserResponse DTOs.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserResponseMapper {

    /**
     * Converts User entity to UserResponse DTO.
     * Password automatically excluded (not mapped to UserResponse).
     * @param user user entity
     * @return user response DTO
     */
    UserResponse toResponse(User user);

}