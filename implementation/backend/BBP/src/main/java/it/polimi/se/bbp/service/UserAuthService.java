package it.polimi.se.bbp.service;

import it.polimi.se.bbp.dto.response.UserAuthResponse;
import it.polimi.se.bbp.dto.request.UserLoginRequest;
import it.polimi.se.bbp.dto.request.UserRegisterRequest;
import it.polimi.se.bbp.entity.User;
import it.polimi.se.bbp.mapper.response.UserAuthResponseMapper;
import it.polimi.se.bbp.mapper.entity.UserMapper;
import it.polimi.se.bbp.repository.UserRepository;
import it.polimi.se.bbp.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for handling user authentication operations.
 * Manages user registration and login processes with manual password validation.
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
     * Password encoder for validating passwords.
     */
    private final PasswordEncoder passwordEncoder;

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
        // Check username and email
        validateUserUniqueness(request);
        // Save user in database
        User user = userRepository.save(userMapper.toEntity(request));
        // Generate JWT token with user ID
        String token = jwtService.generateToken(user.getId());
        return userAuthResponseMapper.toResponse(user, token);
    }

    /**
     * Authenticates a user and generates a JWT token.
     * Uses manual password validation for simplicity and performance.
     * @param request login request containing email and password
     * @return authentication response with JWT token
     * @throws BadCredentialsException if credentials are invalid
     */
    public UserAuthResponse login(UserLoginRequest request) {
        // Query to find user by email
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        // Password validation
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword()))
            throw new BadCredentialsException("Invalid email or password");
        // Generate JWT token with user ID
        String token = jwtService.generateToken(user.getId());
        return userAuthResponseMapper.toResponse(user, token);
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