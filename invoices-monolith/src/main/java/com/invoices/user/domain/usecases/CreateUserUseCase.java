package com.invoices.user.domain.usecases;

import com.invoices.user.domain.entities.User;
import com.invoices.user.domain.ports.PasswordHasher;
import com.invoices.user.domain.ports.UserRepository;
import com.invoices.user.exception.UserAlreadyExistsException;

import java.util.Set;

/**
 * Use case for creating a new user in the system.
 * Encapsulates all business logic for user creation.
 */
public class CreateUserUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public CreateUserUseCase(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    /**
     * Execute the use case to create a new user
     *
     * @param email the user's email (must be unique)
     * @param plainPassword the user's plain text password
     * @param firstName the user's first name
     * @param lastName the user's last name
     * @param roles the user's roles
     * @return the created user with generated ID
     * @throws UserAlreadyExistsException if email already exists
     */
    public User execute(String email, String plainPassword, String firstName,
                       String lastName, Set<String> roles) {
        // Business rule: Email must be unique
        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException(
                "User with email '" + email + "' already exists"
            );
        }

        // Hash the password
        String hashedPassword = passwordHasher.hash(plainPassword);

        // Create domain entity
        User user = new User(email, hashedPassword, firstName, lastName, roles);

        // Persist and return
        return userRepository.save(user);
    }
}
