package it.polimi.se.bbp.controller;

import it.polimi.se.bbp.dto.request.UserUpdateRequest;
import it.polimi.se.bbp.dto.response.UserResponse;
import it.polimi.se.bbp.entity.User;
import it.polimi.se.bbp.mapper.response.UserResponseMapper;
import it.polimi.se.bbp.service.UserAuthService;
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
     * Service for authentication operations.
     */
    private final UserAuthService userAuthService;

    /**
     * Mapper for converting User entities to UserResponse DTOs.
     */
    private final UserResponseMapper userResponseMapper;

    /**
     * Retrieves current authenticated user's data.
     * @return user data with HTTP 200 OK
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        User user = userAuthService.getAuthenticatedUser();
        UserResponse response = userResponseMapper.toResponse(user);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * Updates current authenticated user's data.
     * Only provided fields will be updated (partial update).
     * User can only modify their own data.
     * @param request update request containing fields to modify
     * @return updated user data with HTTP 200 OK
     */
    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateCurrentUser(@Valid @RequestBody UserUpdateRequest request) {
        User user = userService.updateCurrentUser(request);
        UserResponse response = userResponseMapper.toResponse(user);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * Deletes current authenticated user's account.
     * All associated data deleted according to cascade rules.
     * JWT token becomes invalid after deletion.
     * @return HTTP 204 NO CONTENT
     */
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteCurrentUser() {
        userService.deleteCurrentUser();
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}