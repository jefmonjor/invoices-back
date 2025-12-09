package com.invoices.auth.application.services;

import com.invoices.auth.domain.entities.PasswordResetToken;
import com.invoices.auth.domain.ports.PasswordResetTokenRepository;
import com.invoices.shared.domain.ports.EmailService;
import com.invoices.user.domain.entities.User;
import com.invoices.user.domain.ports.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for password reset operations.
 *
 * REFACTORED: Now uses ports instead of infrastructure repositories.
 * Imports only from domain layer, not infrastructure.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${security.token.password-reset-expiration-hours:1}")
    private int tokenExpirationHours;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    /**
     * Initiates a password reset by generating a token and sending an email.
     *
     * @param email the user's email
     */
    @Transactional
    public void initiatePasswordReset(String email) {
        log.info("Password reset requested for email: {}", email);

        // Find user by email
        var userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            // Don't reveal if email exists or not (security best practice)
            log.warn("Password reset requested for non-existent email: {}", email);
            return;
        }

        User user = userOpt.get();

        // Invalidate any existing tokens for this user
        tokenRepository.deleteAllByUserId(user.getId());

        // Generate new token
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .userId(user.getId())
                .token(UUID.randomUUID())
                .expiresAt(LocalDateTime.now().plusHours(tokenExpirationHours))
                .used(false)
                .build();

        tokenRepository.save(resetToken);

        // Send email
        sendPasswordResetEmail(user, resetToken.getToken().toString());

        log.info("Password reset token created for user ID: {}", user.getId());
    }

    /**
     * Verifies if a reset token is valid.
     *
     * @param token the token to verify
     * @return true if the token is valid
     */
    public boolean isTokenValid(UUID token) {
        var tokenOpt = tokenRepository.findValidToken(token);
        return tokenOpt.isPresent();
    }

    /**
     * Resets the password using a valid token.
     *
     * @param token       the reset token
     * @param newPassword the new password
     * @throws IllegalArgumentException if token is invalid or expired
     */
    @Transactional
    public void resetPassword(UUID token, String newPassword) {
        log.info("Attempting to reset password with token");

        // Find and validate token
        PasswordResetToken resetToken = tokenRepository.findValidToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset token"));

        // Find user
        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new IllegalStateException("User not found for reset token"));

        // Update password
        User updatedUser = user.withPassword(passwordEncoder.encode(newPassword));
        userRepository.save(updatedUser);

        // Mark token as used
        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        log.info("Password successfully reset for user ID: {}", user.getId());
    }

    /**
     * Sends the password reset email to the user.
     *
     * @param user  the user
     * @param token the reset token
     */
    private void sendPasswordResetEmail(User user, String token) {
        String resetUrl = frontendUrl + "/reset-password?token=" + token;

        java.util.Map<String, Object> variables = new java.util.HashMap<>();
        variables.put("resetUrl", resetUrl);
        variables.put("tokenExpirationHours", tokenExpirationHours);

        log.debug("Sending password reset email to {}", user.getEmail());
        emailService.sendHtmlEmail(user.getEmail(), "Restablece tu contraseña", "reset-password", variables);
    }

    /**
     * Cleanup job to delete expired and used tokens.
     * Should be called periodically by a scheduled task.
     */
    @Transactional
    public void cleanupExpiredTokens() {
        int deletedCount = tokenRepository.deleteExpiredAndUsedTokens(LocalDateTime.now());
        if (deletedCount > 0) {
            log.info("Cleaned up {} expired/used password reset tokens", deletedCount);
        }
    }
}
