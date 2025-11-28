package com.invoices.user.domain.usecases;

import com.invoices.user.domain.entities.User;
import com.invoices.user.domain.ports.UserRepository;

import java.util.List;

/**
 * Use case for retrieving all users.
 */
public class GetAllUsersUseCase {

    private final UserRepository userRepository;

    public GetAllUsersUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Execute the use case to get all users with pagination
     *
     * @param pageable pagination information
     * @return page of users
     */
    public org.springframework.data.domain.Page<User> execute(org.springframework.data.domain.Pageable pageable) {
        return userRepository.findAll(pageable);
    }
}
