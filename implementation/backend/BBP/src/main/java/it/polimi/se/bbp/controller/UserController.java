package it.polimi.se.bbp.controller;

import it.polimi.se.bbp.dto.request.UserUpdateRequest;
import it.polimi.se.bbp.dto.response.UserResponse;
import it.polimi.se.bbp.entity.User;
import it.polimi.se.bbp.mapper.response.UserResponseMapper;
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
     *
     */
    private final UserResponseMapper userResponseMapper;

    /**
     * Retrieves the current authenticated user's data.
     * @return ResponseEntity with HTTP 200 OK status and user data
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        User user = userService.getCurrentUser();
        UserResponse response = userResponseMapper.toResponse(user);
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
        User user = userService.updateCurrentUser(request);
        UserResponse response = userResponseMapper.toResponse(user);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * Deletes the current authenticated user's account.
     * All associated data (trips, bike paths, etc.) will be deleted according to cascade rules.
     * After deletion, the JWT token will become invalid automatically.
     * @return ResponseEntity with HTTP 204 NO CONTENT status
     */
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteCurrentUser() {
        userService.deleteCurrentUser();
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}