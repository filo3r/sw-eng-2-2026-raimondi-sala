package it.polimi.se.bbp.service;

import it.polimi.se.bbp.dto.request.UserUpdateRequest;
import it.polimi.se.bbp.dto.response.UserResponse;
import it.polimi.se.bbp.entity.User;
import it.polimi.se.bbp.mapper.UserResponseMapper;
import it.polimi.se.bbp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing user operations.
 * Handles user profile updates and data retrieval.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    /**
     * Repository for user data access operations.
     */
    private final UserRepository userRepository;

    /**
     * Password encoder for hashing passwords.
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * Mapper for converting User entities to UserResponse DTOs.
     */
    private final UserResponseMapper userResponseMapper;

    /**
     * Updates the current authenticated user's data.
     * Only non-null fields in the request will be updated.
     * @param request update request containing fields to modify
     * @return updated user data
     * @throws IllegalStateException if authenticated user is not found in database
     * @throws IllegalArgumentException if username or email already exists
     */
    @Transactional
    public UserResponse updateCurrentUser(UserUpdateRequest request) {
        // Get current authenticated user ID from SecurityContext
        Long currentUserId = getCurrentUserId();
        User user = userRepository.findById(currentUserId).orElseThrow(() -> new IllegalStateException("Authenticated user not found in database"));
        // Update only non-null fields
        updateUserFields(user, request);
        // Save and return updated user
        User updatedUser = userRepository.save(user);
        return userResponseMapper.toResponse(updatedUser);
    }

    /**
     * Updates the user entity with non-null fields from the request.
     * Validates uniqueness constraints for username and email before updating.
     * Passwords are hashed before being stored.
     * @param user the user entity to update
     * @param request the update request containing new values
     * @throws IllegalArgumentException if username or email already exists
     */
    private void updateUserFields(User user, UserUpdateRequest request) {
        // Update name
        if (request.getName() != null)
            user.setName(request.getName());
        // Update surname
        if (request.getSurname() != null)
            user.setSurname(request.getSurname());
        // Update username
        validateAndUpdateUsername(user, request.getUsername());
        // Update email
        validateAndUpdateEmail(user, request.getEmail());
        // Update password
        if (request.getPassword() != null)
            user.setPassword(passwordEncoder.encode(request.getPassword()));
    }

    /**
     * Validates and updates the username if it's different from the current one.
     * Checks for username uniqueness before updating.
     * @param user the user entity to update
     * @param newUsername the new username to set
     * @throws IllegalArgumentException if the new username is already in use
     */
    private void validateAndUpdateUsername(User user, String newUsername) {
        if (newUsername != null && !newUsername.equals(user.getUsername())) {
            if (userRepository.existsByUsername(newUsername))
                throw new IllegalArgumentException("Username is already in use");
            user.setUsername(newUsername);
        }
    }

    /**
     * Validates and updates the email if it's different from the current one.
     * Checks for email uniqueness before updating.
     * @param user the user entity to update
     * @param newEmail the new email to set
     * @throws IllegalArgumentException if the new email is already in use
     */
    private void validateAndUpdateEmail(User user, String newEmail) {
        if (newEmail != null && !newEmail.equals(user.getEmail())) {
            if (userRepository.existsByEmail(newEmail))
                throw new IllegalArgumentException("Email is already in use");
            user.setEmail(newEmail);
        }
    }

    /**
     * Retrieves the current authenticated user's data.
     * @return current user data
     * @throws IllegalStateException if authenticated user is not found in database
     */
    public UserResponse getCurrentUser() {
        Long currentUserId = getCurrentUserId();
        User user = userRepository.findById(currentUserId).orElseThrow(() -> new IllegalStateException("Authenticated user not found in database"));
        return userResponseMapper.toResponse(user);
    }

    /**
     * Deletes the current authenticated user's account.
     * All associated data will be deleted according to cascade rules defined in User entity:
     * - Recorded trips: deleted (cascade delete)
     * - Created bike paths: deleted (cascade delete)
     * - Created obstacles: preserved (creator set to null)
     * - Updates to bike paths/obstacles: preserved
     * After deletion, the JWT token will become invalid automatically as the user will no longer exist in the database.
     * @throws IllegalStateException if authenticated user is not found in database
     */
    @Transactional
    public void deleteCurrentUser() {
        Long currentUserId = getCurrentUserId();
        User user = userRepository.findById(currentUserId).orElseThrow(() -> new IllegalStateException("Authenticated user not found in database"));
        userRepository.delete(user);
    }

    /**
     * Extracts the user ID of the currently authenticated user from the security context.
     * The userId is stored as principal by JwtAuthFilter after validating the JWT token.
     * @return user ID of the authenticated user
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (Long) authentication.getPrincipal();
    }

}