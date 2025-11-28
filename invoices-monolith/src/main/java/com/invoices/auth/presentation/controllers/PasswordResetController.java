package com.invoices.auth.presentation.controllers;

import com.invoices.auth.application.dto.ForgotPasswordRequest;
import com.invoices.auth.application.dto.ResetPasswordRequest;
import com.invoices.auth.application.services.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for password reset operations.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Password Reset", description = "Endpoints for password reset functionality")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    /**
     * Initiates password reset process by sending an email with a reset token.
     *
     * @param request the forgot password request
     * @return success message
     */
    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot password", description = "Initiates password reset by sending an email with a reset token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset email sent (if email exists)", content = @Content),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content)
    })
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("POST /api/auth/forgot-password - Password reset requested for: {}", request.getEmail());

        passwordResetService.initiatePasswordReset(request.getEmail());

        // Always return success to prevent email enumeration
        return ResponseEntity.ok(Map.of(
                "message", "If an account with that email exists, a password reset link has been sent."));
    }

    /**
     * Verifies if a reset token is valid.
     *
     * @param token the reset token
     * @return validity status
     */
    @GetMapping("/verify-reset-token/{token}")
    @Operation(summary = "Verify reset token", description = "Checks if a password reset token is valid")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token is valid", content = @Content),
            @ApiResponse(responseCode = "400", description = "Token is invalid or expired", content = @Content)
    })
    public ResponseEntity<Map<String, Boolean>> verifyResetToken(@PathVariable String token) {
        log.info("GET /api/auth/verify-reset-token/{}", token);

        try {
            UUID tokenUuid = UUID.fromString(token);
            boolean isValid = passwordResetService.isTokenValid(tokenUuid);

            return ResponseEntity.ok(Map.of("valid", isValid));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(Map.of("valid", false));
        }
    }

    /**
     * Resets password using a valid reset token.
     *
     * @param request the reset password request
     * @return success message
     */
    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Resets password using a valid reset token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset successful", content = @Content),
            @ApiResponse(responseCode = "400", description = "Invalid or expired token", content = @Content)
    })
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("POST /api/auth/reset-password - Resetting password with token");

        try {
            passwordResetService.resetPassword(request.getTokenAsUUID(), request.getNewPassword());

            return ResponseEntity.ok(Map.of(
                    "message", "Password has been reset successfully. You can now login with your new password."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid or expired reset token."));
        }
    }
}
