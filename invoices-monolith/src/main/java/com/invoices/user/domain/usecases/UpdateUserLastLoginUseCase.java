package com.invoices.user.domain.usecases;

import com.invoices.user.domain.entities.User;
import com.invoices.user.domain.ports.UserRepository;
import com.invoices.user.exception.UserNotFoundException;

import java.time.LocalDateTime;

/**
 * Use case for updating a user's last login timestamp.
 */
public class UpdateUserLastLoginUseCase {

    private final UserRepository userRepository;

    public UpdateUserLastLoginUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Execute the use case to update last login
     *
     * @param userId the user ID
     * @return the updated user
     * @throws UserNotFoundException if user doesn't exist
     */
    public User execute(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(
                    "User not found with ID: " + userId
                ));

        // Create new user instance with updated last login
        User updatedUser = user.withLastLogin(LocalDateTime.now());

        // Persist and return
        return userRepository.save(updatedUser);
    }
}
