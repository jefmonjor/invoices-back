package com.invoices.user.domain.usecases;

import com.invoices.user.domain.ports.UserRepository;
import com.invoices.user.exception.UserNotFoundException;

/**
 * Use case for deleting a user.
 */
public class DeleteUserUseCase {

    private final UserRepository userRepository;

    public DeleteUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Execute the use case to delete a user
     *
     * @param userId the user ID to delete
     * @throws UserNotFoundException if user doesn't exist
     */
    public void execute(Long userId) {
        // Business rule: Verify user exists before deleting
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(
                "Cannot delete user: User not found with ID: " + userId
            );
        }

        userRepository.deleteById(userId);
    }
}
