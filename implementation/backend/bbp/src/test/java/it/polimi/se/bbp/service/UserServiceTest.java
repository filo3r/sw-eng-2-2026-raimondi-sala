package it.polimi.se.bbp.service;

import it.polimi.se.bbp.dto.request.UserUpdateRequest;
import it.polimi.se.bbp.entity.User;
import it.polimi.se.bbp.exception.user.UserAlreadyExistsException;
import it.polimi.se.bbp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("User Service Update and Delete Tests")
class UserServiceTest {

    @Mock
    private UserAuthService userAuthService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User currentUser;

    @BeforeEach
    void setUp() {
        // Initialize the base user
        currentUser = User.builder()
                .id(1L)
                .name("Mario")
                .surname("Rossi")
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .build();
    }

    @Test
    @DisplayName("Should update all user fields when valid data is provided")
    void updateCurrentUser_AllFields_Success() {
        // Simulate a request modifying all available fields to new identity Luigi Verdi
        UserUpdateRequest request = new UserUpdateRequest(
                "Luigi",
                "Verdi",
                "newuser",
                "luigi@example.com",
                "newPassword123"
        );

        // Service retrieves the authenticated Mario Rossi user
        when(userAuthService.getAuthenticatedUser()).thenReturn(currentUser);

        // Mock repository to show new username and email are available
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("luigi@example.com")).thenReturn(false);

        // Mock password hashing for the new password
        when(passwordEncoder.encode("newPassword123")).thenReturn("hashedNewPass");

        // The save method must return the updated user entity
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);

        User updatedUser = userService.updateCurrentUser(request);

        // Verify that all fields were correctly modified on the entity
        assertEquals("Luigi", updatedUser.getName());
        assertEquals("Verdi", updatedUser.getSurname());
        assertEquals("newuser", updatedUser.getUsername());
        assertEquals("luigi@example.com", updatedUser.getEmail());
        assertEquals("hashedNewPass", updatedUser.getPassword());

        verify(userRepository).save(currentUser);
    }

    @Test
    @DisplayName("Should update only non null fields and preserve existing data")
    void updateCurrentUser_PartialUpdate_Success() {
        // Send a request with only the name modified while leaving other fields null
        UserUpdateRequest request = new UserUpdateRequest(
                "Luigi", null, null, null, null
        );

        when(userAuthService.getAuthenticatedUser()).thenReturn(currentUser);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);

        User updatedUser = userService.updateCurrentUser(request);

        // Verify the name changed to Luigi but surname remains Rossi
        assertEquals("Luigi", updatedUser.getName());
        assertEquals("Rossi", updatedUser.getSurname());

        // Ensure encoder and uniqueness checks were not triggered for null fields
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).existsByEmail(anyString());
    }

    @Test
    @DisplayName("Should throw UserAlreadyExistsException when the new username is already taken")
    void updateCurrentUser_UsernameTaken_ThrowsException() {
        // Attempt to change the username to one that already exists
        UserUpdateRequest request = new UserUpdateRequest(
                null, null, "existing_user", null, null
        );

        when(userAuthService.getAuthenticatedUser()).thenReturn(currentUser);
        when(userRepository.existsByUsername("existing_user")).thenReturn(true);

        // Expect the service to block operation with a custom exception
        assertThrows(UserAlreadyExistsException.class, () -> userService.updateCurrentUser(request));

        // Ensure no save attempt is made
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw UserAlreadyExistsException when the new email is already taken")
    void updateCurrentUser_EmailTaken_ThrowsException() {
        // Attempt to change the email to one currently in use
        UserUpdateRequest request = new UserUpdateRequest(
                null, null, null, "occupied@example.com", null
        );

        when(userAuthService.getAuthenticatedUser()).thenReturn(currentUser);
        when(userRepository.existsByEmail("occupied@example.com")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> userService.updateCurrentUser(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should skip uniqueness check when the email remains unchanged")
    void updateCurrentUser_SameEmail_SkipsCheck() {
        // Submitting an update with the same email address test@example.com
        // The system must skip the existence check for this specific email
        UserUpdateRequest request = new UserUpdateRequest(
                null, null, null, "test@example.com", null
        );

        when(userAuthService.getAuthenticatedUser()).thenReturn(currentUser);
        when(userRepository.save(any(User.class))).thenReturn(currentUser);

        userService.updateCurrentUser(request);

        // Verify the existsByEmail method was never called
        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository).save(currentUser);
    }

    @Test
    @DisplayName("Should successfully delete the authenticated user account")
    void deleteCurrentUser_Success() {
        // Test account deletion logic for Mario Rossi
        when(userAuthService.getAuthenticatedUser()).thenReturn(currentUser);

        userService.deleteCurrentUser();

        // Verify the delete method is invoked with the retrieved user entity
        verify(userRepository).delete(currentUser);
    }
}