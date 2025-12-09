package it.polimi.se.bbp.mapper.entity;

import it.polimi.se.bbp.dto.request.UserRegisterRequest;
import it.polimi.se.bbp.entity.User;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * Mapper for converting user registration requests to User entities.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        builder = @Builder(disableBuilder = true))
public interface UserMapper {

    /**
     * Converts UserRegisterRequest to User entity.
     * Password must be pre-encoded before calling this method.
     * @param request registration request
     * @param encodedPassword already encoded password string
     * @return user entity
     */
    @Mapping(target = "password", source = "encodedPassword")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "recordedTrips", ignore = true)
    @Mapping(target = "createdBikePaths", ignore = true)
    @Mapping(target = "updatedBikePaths", ignore = true)
    @Mapping(target = "createdObstacles", ignore = true)
    @Mapping(target = "updatedObstacles", ignore = true)
    User toEntity(UserRegisterRequest request, String encodedPassword);

}