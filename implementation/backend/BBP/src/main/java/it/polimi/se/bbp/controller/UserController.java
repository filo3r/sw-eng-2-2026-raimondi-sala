package it.polimi.se.bbp.controller;

import it.polimi.se.bbp.dto.request.UserUpdateRequest;
import it.polimi.se.bbp.dto.response.UserResponse;
import it.polimi.se.bbp.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for authenticated user operations.
 * Handles user profile retrieval and updates.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    /**
     * Service for handling user operations.
     */
    private final UserService userService;

    /**
     * Retrieves the current authenticated user's data.
     * @return ResponseEntity with HTTP 200 OK status and user data
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        UserResponse response = userService.getCurrentUser();
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * Updates the current authenticated user's data.
     * Only provided fields will be updated (partial update).
     * The user can only modify their own data.
     * @param request the update request containing fields to modify
     * @return ResponseEntity with HTTP 200 OK status and updated user data
     */
    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateCurrentUser(@Valid @RequestBody UserUpdateRequest request) {
        UserResponse response = userService.updateCurrentUser(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

}