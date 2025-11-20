package com.invoices.user.presentation.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for user login request.
 * Accepts both "email" and "username" fields (for compatibility with frontend).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @JsonAlias("username")  // Accept "username" field from frontend
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    /**
     * Trim email to remove leading/trailing whitespace.
     */
    public void setEmail(String email) {
        this.email = email != null ? email.trim() : null;
    }

    /**
     * Trim password to remove leading/trailing whitespace.
     */
    public void setPassword(String password) {
        this.password = password != null ? password.trim() : null;
    }
}
