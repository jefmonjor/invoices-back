package com.invoices.auth.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for resetting a password with a valid token.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordRequest {

    @NotBlank(message = "Token is required")
    private String token;

    @NotBlank(message = "New password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String newPassword;

    public UUID getTokenAsUUID() {
        try {
            return UUID.fromString(token);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid token format");
        }
    }
}
