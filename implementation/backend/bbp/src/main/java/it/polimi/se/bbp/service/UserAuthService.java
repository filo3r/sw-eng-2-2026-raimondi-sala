package it.polimi.se.bbp.service;

import it.polimi.se.bbp.dto.response.UserAuthResponse;
import it.polimi.se.bbp.dto.request.UserLoginRequest;
import it.polimi.se.bbp.dto.request.UserRegisterRequest;
import it.polimi.se.bbp.entity.User;
import it.polimi.se.bbp.exception.user.UserAlreadyExistsException;
import it.polimi.se.bbp.mapper.response.UserAuthResponseMapper;
import it.polimi.se.bbp.mapper.entity.UserMapper;
import it.polimi.se.bbp.repository.UserRepository;
import it.polimi.se.bbp.security.JwtService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for user authentication operations.
 * Manages registration and login with manual password validation.
 */
@Service
@RequiredArgsConstructor
public class UserAuthService {

    /**
     * Repository for user data access.
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
     * Mapper for converting registration requests to User entities.
     */
    private final UserMapper userMapper;

    /**
     * Mapper for converting User entities to authentication response DTOs.
     */
    private final UserAuthResponseMapper userAuthResponseMapper;

    /**
     * Registers new user in system.
     * @param request registration request with user details
     * @return authentication response with JWT token
     * @throws UserAlreadyExistsException if username or email already exists
     */
    @Transactional
    public UserAuthResponse register(UserRegisterRequest request) {
        validateUserUniqueness(request);
        String encodedPassword = passwordEncoder.encode(request.password());
        User user = userRepository.save(userMapper.toEntity(request, encodedPassword));
        String token = jwtService.generateToken(user.getId());
        return userAuthResponseMapper.toResponse(user, token);
    }

    /**
     * Authenticates user and generates JWT token.
     * Uses manual password validation for simplicity and performance.
     * @param request login request with email and password
     * @return authentication response with JWT token
     * @throws BadCredentialsException if credentials invalid
     */
    public UserAuthResponse login(UserLoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.getPassword()))
            throw new BadCredentialsException("Invalid email or password");
        String token = jwtService.generateToken(user.getId());
        return userAuthResponseMapper.toResponse(user, token);
    }

    /**
     * Retrieves fully authenticated User entity from database.
     * Preferred method for other services to get current user.
     * @return authenticated User entity
     * @throws EntityNotFoundException if user ID from token doesn't exist in database
     */
    public User getAuthenticatedUser() {
        Long currentUserId = getCurrentUserId();
        return userRepository.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("Authenticated user not found"));
    }

    /**
     * Validates username and email are unique in system.
     * @param request registration request with username and email
     * @throws UserAlreadyExistsException if username or email already exists
     */
    private void validateUserUniqueness(UserRegisterRequest request) {
        if (userRepository.existsByUsername(request.username()))
            throw new UserAlreadyExistsException("Username is already in use");
        if (userRepository.existsByEmail(request.email()))
            throw new UserAlreadyExistsException("Email is already in use");
    }

    /**
     * Retrieves authenticated user's ID from security context.
     * @return user ID
     */
    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

}