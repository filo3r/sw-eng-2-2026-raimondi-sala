package it.polimi.se.bbp.mapper.entity;

import it.polimi.se.bbp.dto.request.UserRegisterRequest;
import it.polimi.se.bbp.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting user registration requests to User entities.
 */
@Component
@RequiredArgsConstructor
public class UserMapper {

    /**
     * Password encoder for encoding user passwords.
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * Converts a UserRegisterRequest to a User entity.
     * The password is encoded before setting it.
     * @param request the registration request
     * @return the user entity
     */
    public User toEntity(UserRegisterRequest request) {
        return User.builder()
                .name(request.getName())
                .surname(request.getSurname())
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();
    }

}