package it.polimi.se.bbp.mapper.response;

import it.polimi.se.bbp.dto.response.UserAuthResponse;
import it.polimi.se.bbp.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * Mapper for converting User entities to authentication response DTOs.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserAuthResponseMapper {

    /**
     * Converts User entity and JWT token to UserAuthResponse DTO.
     * @param user user entity
     * @param token JWT token
     * @return authentication response DTO
     */
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "token", source = "token")
    UserAuthResponse toResponse(User user, String token);

}