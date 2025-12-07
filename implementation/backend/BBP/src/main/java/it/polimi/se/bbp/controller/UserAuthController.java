package it.polimi.se.bbp.controller;

import it.polimi.se.bbp.dto.request.UserLoginRequest;
import it.polimi.se.bbp.dto.request.UserRegisterRequest;
import it.polimi.se.bbp.dto.response.UserAuthResponse;
import it.polimi.se.bbp.service.UserAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for user authentication.
 * Handles user registration and login operations.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserAuthController {

    /**
     * Service for user authentication business logic.
     */
    private final UserAuthService userAuthService;

    /**
     * Registers a new user in the system.
     * Creates user account, encrypts password, and generates JWT token for immediate login.
     * @param request registration data including email, password, and user details
     * @return authentication response with JWT token
     */
    @PostMapping("/register")
    public ResponseEntity<UserAuthResponse> register(@Valid @RequestBody UserRegisterRequest request) {
        UserAuthResponse response = userAuthService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Authenticates a user and generates a JWT token.
     * Validates credentials and returns token for subsequent authenticated requests.
     * @param request login credentials including email and password
     * @return authentication response with JWT token
     */
    @PostMapping("/login")
    public ResponseEntity<UserAuthResponse> login(@Valid @RequestBody UserLoginRequest request) {
        UserAuthResponse response = userAuthService.login(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

}