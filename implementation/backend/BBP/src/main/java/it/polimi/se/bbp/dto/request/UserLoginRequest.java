package it.polimi.se.bbp.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for user login request.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserLoginRequest {

    /**
     * User email for login.
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    /**
     * User password.
     */
    @NotBlank(message = "Password is required")
    private String password;

}