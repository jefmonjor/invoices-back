package com.invoices.user.domain.usecases;

import com.invoices.user.domain.entities.User;
import com.invoices.user.domain.ports.UserRepository;
import com.invoices.user.exception.UserNotFoundException;

/**
 * Use case for retrieving a user by email.
 */
public class GetUserByEmailUseCase {

    private final UserRepository userRepository;

    public GetUserByEmailUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Execute the use case to get a user by email
     *
     * @param email the user email
     * @return the user
     * @throws UserNotFoundException if user doesn't exist
     */
    public User execute(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(
                        "User not found with email: " + email));
    }
}
