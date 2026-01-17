package it.polimi.se.bbp.service;

import it.polimi.se.bbp.dto.request.UserLoginRequest;
import it.polimi.se.bbp.dto.request.UserRegisterRequest;
import it.polimi.se.bbp.dto.response.UserAuthResponse;
import it.polimi.se.bbp.entity.User;
import it.polimi.se.bbp.exception.user.UserAlreadyExistsException;
import it.polimi.se.bbp.mapper.entity.UserMapper;
import it.polimi.se.bbp.mapper.response.UserAuthResponseMapper;
import it.polimi.se.bbp.repository.UserRepository;
import it.polimi.se.bbp.security.JwtService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("User Authentication Service Tests")
class UserAuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserMapper userMapper;
    @Mock
    private UserAuthResponseMapper userAuthResponseMapper;
    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserAuthService userAuthService;

    private User mockUser;
    private UserRegisterRequest registerRequest;
    private UserLoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        // Initialize the test data
        mockUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .username("testuser")
                .password("encodedPassword")
                .build();

        registerRequest = new UserRegisterRequest(
                "Mario", "Rossi", "testuser", "test@example.com", "password123"
        );

        loginRequest = new UserLoginRequest("test@example.com", "password123");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should successfully register a new user and return an auth token")
    void register_Success() {
        // We simulate a fresh registration where the username and email do not exist yet
        when(userRepository.existsByUsername(registerRequest.username())).thenReturn(false);
        when(userRepository.existsByEmail(registerRequest.email())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.password())).thenReturn("encodedPassword");

        when(userMapper.toEntity(any(UserRegisterRequest.class), eq("encodedPassword"))).thenReturn(mockUser);
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(jwtService.generateToken(mockUser.getId())).thenReturn("mockToken");

        when(userAuthResponseMapper.toResponse(mockUser, "mockToken"))
                .thenReturn(new UserAuthResponse("mockToken", 1L));

        UserAuthResponse response = userAuthService.register(registerRequest);

        // The service should return the token and userId, and the user must be saved to the DB
        assertNotNull(response);
        assertEquals("mockToken", response.token());
        assertEquals(1L, response.userId());

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw UserAlreadyExistsException when the username is already taken")
    void register_ThrowsWhenUsernameExists() {
        // If the username is already taken, the service must block the registration
        when(userRepository.existsByUsername(registerRequest.username())).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> userAuthService.register(registerRequest));

        // Ensure we never attempted to save the duplicate user
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw UserAlreadyExistsException when the email is already in use")
    void register_ThrowsWhenEmailExists() {
        // Similarly, if the email already exists, registration must fail
        when(userRepository.existsByUsername(registerRequest.username())).thenReturn(false);
        when(userRepository.existsByEmail(registerRequest.email())).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> userAuthService.register(registerRequest));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should login successfully and return JWT token when credentials are valid")
    void login_Success() {
        // When valid credentials are provided, the service should authenticate the user and return a token
        when(userRepository.findByEmail(loginRequest.email())).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(loginRequest.password(), mockUser.getPassword())).thenReturn(true);
        when(jwtService.generateToken(mockUser.getId())).thenReturn("mockToken");
        when(userAuthResponseMapper.toResponse(mockUser, "mockToken"))
                .thenReturn(new UserAuthResponse("mockToken", 1L));

        UserAuthResponse response = userAuthService.login(loginRequest);

        assertNotNull(response);
        assertEquals("mockToken", response.token());
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when the email does not exist")
    void login_ThrowsWhenUserNotFound() {
        // If the user is not found, we expect a BadCredentialsException
        when(userRepository.findByEmail(loginRequest.email())).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> userAuthService.login(loginRequest));
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when the password does not match")
    void login_ThrowsWhenPasswordMismatch() {
        // Even if the user exists, a wrong password must trigger a BadCredentialsException
        when(userRepository.findByEmail(loginRequest.email())).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(loginRequest.password(), mockUser.getPassword())).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> userAuthService.login(loginRequest));
    }

    @Test
    @DisplayName("Should retrieve the current authenticated user from the SecurityContext")
    void getAuthenticatedUser_Success() {
        // Simulates a scenario where the SecurityContext holds a valid User ID
        setupSecurityContext(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

        User result = userAuthService.getAuthenticatedUser();

        assertNotNull(result);
        assertEquals(mockUser.getId(), result.getId());
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException if authenticated user is missing from DB")
    void getAuthenticatedUser_ThrowsWhenUserDeleted() {
        // If the token is valid but the user was deleted from the DB, we expect an EntityNotFoundException
        setupSecurityContext(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> userAuthService.getAuthenticatedUser());
    }

    @Test
    @DisplayName("Should return authenticated user or continue safely if found")
    void getAuthenticatedUserOrNull_Success() {
        // This method works like the standard getter but handles missing users gracefully
        setupSecurityContext(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

        User result = userAuthService.getAuthenticatedUserOrNull();

        assertNotNull(result);
        assertEquals(mockUser.getId(), result.getId());
    }

    @Test
    @DisplayName("Should return null when the security context is anonymous")
    void getAuthenticatedUserOrNull_ReturnsNullWhenNoAuth() {
        // If no user is logged in, it should return null instead of throwing
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(null);

        User result = userAuthService.getAuthenticatedUserOrNull();

        assertNull(result);
    }

    // Helper method to mock the static SecurityContextHolder for authenticated scenarios
    private void setupSecurityContext(Long userId) {
        SecurityContextHolder.setContext(securityContext);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getPrincipal()).thenReturn(userId);
    }
}