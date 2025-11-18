package com.invoices.user.domain.usecases;

import com.invoices.user.domain.entities.User;
import com.invoices.user.domain.ports.UserRepository;
import com.invoices.user.exception.UserNotFoundException;

/**
 * Use case for retrieving a user by ID.
 */
public class GetUserByIdUseCase {

    private final UserRepository userRepository;

    public GetUserByIdUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Execute the use case to get a user by ID
     *
     * @param userId the user ID
     * @return the user
     * @throws UserNotFoundException if user doesn't exist
     */
    public User execute(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(
                    "User not found with ID: " + userId
                ));
    }
}
