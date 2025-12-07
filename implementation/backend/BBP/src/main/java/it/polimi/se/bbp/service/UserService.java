package it.polimi.se.bbp.service;

import it.polimi.se.bbp.dto.request.UserUpdateRequest;
import it.polimi.se.bbp.entity.User;
import it.polimi.se.bbp.exception.user.UserAlreadyExistsException;
import it.polimi.se.bbp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for user profile operations.
 * Handles profile updates and account deletion.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    /**
     * Service for user authentication operations.
     */
    private final UserAuthService userAuthService;

    /**
     * Repository for user data access.
     */
    private final UserRepository userRepository;

    /**
     * Password encoder for hashing passwords.
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * Updates current authenticated user's profile.
     * Only non-null fields updated.
     * @param request update request with fields to modify
     * @return updated user entity
     * @throws UserAlreadyExistsException if username or email already exists
     */
    @Transactional
    public User updateCurrentUser(UserUpdateRequest request) {
        User user = userAuthService.getAuthenticatedUser();
        updateUserFields(user, request);
        return userRepository.save(user);
    }

    /**
     * Deletes current authenticated user's account.
     * Associated data handled per cascade rules: recorded trips and created bike paths deleted,
     * created obstacles preserved (creator set to null).
     * JWT token becomes invalid as user no longer exists in database.
     */
    @Transactional
    public void deleteCurrentUser() {
        User user = userAuthService.getAuthenticatedUser();
        userRepository.delete(user);
    }

    /**
     * Updates user entity with non-null fields from request.
     * Validates uniqueness constraints, hashes passwords before storing.
     * @param user user entity to update
     * @param request update request with new values
     * @throws UserAlreadyExistsException if username or email already exists
     */
    private void updateUserFields(User user, UserUpdateRequest request) {
        if (request.name() != null)
            user.setName(request.name());
        if (request.surname() != null)
            user.setSurname(request.surname());
        validateAndUpdateUsername(user, request.username());
        validateAndUpdateEmail(user, request.email());
        if (request.password() != null)
            user.setPassword(passwordEncoder.encode(request.password()));
    }

    /**
     * Validates and updates username if different from current.
     * Checks uniqueness before updating.
     * @param user user entity to update
     * @param newUsername new username to set
     * @throws UserAlreadyExistsException if username already in use
     */
    private void validateAndUpdateUsername(User user, String newUsername) {
        if (newUsername != null && !newUsername.equals(user.getUsername())) {
            if (userRepository.existsByUsername(newUsername))
                throw new UserAlreadyExistsException("Username is already in use");
            user.setUsername(newUsername);
        }
    }

    /**
     * Validates and updates email if different from current.
     * Checks uniqueness before updating.
     * @param user user entity to update
     * @param newEmail new email to set
     * @throws UserAlreadyExistsException if email already in use
     */
    private void validateAndUpdateEmail(User user, String newEmail) {
        if (newEmail != null && !newEmail.equals(user.getEmail())) {
            if (userRepository.existsByEmail(newEmail))
                throw new UserAlreadyExistsException("Email is already in use");
            user.setEmail(newEmail);
        }
    }

}