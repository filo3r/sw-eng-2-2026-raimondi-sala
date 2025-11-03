package it.polimi.se.bbp.service;

import it.polimi.se.bbp.dto.response.UserAuthResponse;
import it.polimi.se.bbp.dto.request.UserLoginRequest;
import it.polimi.se.bbp.dto.request.UserRegisterRequest;
import it.polimi.se.bbp.entity.User;
import it.polimi.se.bbp.mapper.UserAuthResponseMapper;
import it.polimi.se.bbp.mapper.UserMapper;
import it.polimi.se.bbp.repository.UserRepository;
import it.polimi.se.bbp.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for handling user authentication operations.
 * Manages user registration and login processes.
 */
@Service
@RequiredArgsConstructor
public class UserAuthService {

    /**
     * Repository for user data access operations.
     */
    private final UserRepository userRepository;

    /**
     * Service for JWT token generation and validation.
     */
    private final JwtService jwtService;

    /**
     * Manager for authenticating users with Spring Security.
     */
    private final AuthenticationManager authenticationManager;

    /**
     * Mapper for converting user registration requests to User entities.
     */
    private final UserMapper userMapper;

    /**
     * Mapper for converting User entities to authentication response DTOs.
     */
    private final UserAuthResponseMapper userAuthResponseMapper;

    /**
     * Registers a new user in the system.
     * @param request registration request containing user details
     * @return authentication response with JWT token
     * @throws IllegalArgumentException if username or email already exists
     */
    @Transactional
    public UserAuthResponse register(UserRegisterRequest request) {
        validateUserUniqueness(request);
        User user = userRepository.save(userMapper.toEntity(request));
        String token = jwtService.generateToken(user.getId());
        return userAuthResponseMapper.toResponse(user, token, "User registered successfully");
    }

    /**
     * Authenticates a user and generates a JWT token.
     * User logs in with email, and token contains user ID.
     * @param request login request containing email and password
     * @return authentication response with JWT token
     * @throws IllegalArgumentException if email is invalid
     * @throws org.springframework.security.authentication.BadCredentialsException if credentials are invalid
     */
    public UserAuthResponse login(UserLoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(user.getUsername(), request.getPassword()));
        String token = jwtService.generateToken(user.getId());
        return userAuthResponseMapper.toResponse(user, token, "User logged in successfully");
    }

    /**
     * Validates that username and email are unique in the system.
     * @param request the registration request containing username and email
     * @throws IllegalArgumentException if username or email already exists
     */
    private void validateUserUniqueness(UserRegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername()))
            throw new IllegalArgumentException("Username is already in use");
        if (userRepository.existsByEmail(request.getEmail()))
            throw new IllegalArgumentException("Email is already in use");
    }

}