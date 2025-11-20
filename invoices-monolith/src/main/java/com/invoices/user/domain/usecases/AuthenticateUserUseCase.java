package com.invoices.user.domain.usecases;

import com.invoices.user.domain.entities.User;
import com.invoices.user.domain.ports.PasswordHasher;
import com.invoices.user.domain.ports.UserRepository;
import com.invoices.user.exception.InvalidCredentialsException;
import lombok.extern.slf4j.Slf4j;

/**
 * Use case for authenticating a user with email and password.
 * Encapsulates authentication business logic.
 */
@Slf4j
public class AuthenticateUserUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public AuthenticateUserUseCase(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    /**
     * Execute the use case to authenticate a user
     *
     * @param email the user's email
     * @param plainPassword the user's plain text password
     * @return the authenticated user
     * @throws InvalidCredentialsException if credentials are invalid
     */
    public User execute(String email, String plainPassword) {
        log.debug("Authenticating user with email: {}", email);
        log.debug("Plain password length: {}", plainPassword != null ? plainPassword.length() : 0);

        // Find user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("User not found with email: {}", email);
                    return new InvalidCredentialsException("Invalid email or password");
                });

        log.debug("User found: id={}, email={}, enabled={}", user.getId(), user.getEmail(), user.isEnabled());
        log.debug("Stored password hash: {}", user.getPassword());

        // Business rule: User must be enabled
        if (!user.isEnabled()) {
            log.warn("User account is disabled: {}", email);
            throw new InvalidCredentialsException(
                "User account is disabled"
            );
        }

        // Business rule: Account must be valid (not expired, not locked)
        if (!user.isAccountValid()) {
            log.warn("User account is not valid: {}", email);
            throw new InvalidCredentialsException(
                "User account is not valid (expired, locked, or credentials expired)"
            );
        }

        // Verify password
        log.debug("Attempting password match...");
        boolean matches = passwordHasher.matches(plainPassword, user.getPassword());
        log.debug("Password match result: {}", matches);

        if (!matches) {
            log.warn("Password mismatch for user: {}", email);
            throw new InvalidCredentialsException(
                "Invalid email or password"
            );
        }

        log.info("Authentication successful for user: {}", email);
        return user;
    }
}
