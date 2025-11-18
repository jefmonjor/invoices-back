package com.invoices.user.domain.usecases;

import com.invoices.user.domain.entities.User;
import com.invoices.user.domain.ports.PasswordHasher;
import com.invoices.user.domain.ports.UserRepository;
import com.invoices.user.exception.InvalidCredentialsException;

/**
 * Use case for authenticating a user with email and password.
 * Encapsulates authentication business logic.
 */
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
        // Find user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException(
                    "Invalid email or password"
                ));

        // Business rule: User must be enabled
        if (!user.isEnabled()) {
            throw new InvalidCredentialsException(
                "User account is disabled"
            );
        }

        // Business rule: Account must be valid (not expired, not locked)
        if (!user.isAccountValid()) {
            throw new InvalidCredentialsException(
                "User account is not valid (expired, locked, or credentials expired)"
            );
        }

        // Verify password
        if (!passwordHasher.matches(plainPassword, user.getPassword())) {
            throw new InvalidCredentialsException(
                "Invalid email or password"
            );
        }

        return user;
    }
}
