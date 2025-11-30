package com.invoices.user.domain.usecases;

import com.invoices.user.domain.entities.User;
import com.invoices.user.domain.ports.UserRepository;

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
    /**
     * Execute the use case to get all users with pagination, optionally filtered by
     * company.
     *
     * @param pageable  pagination information
     * @param companyId optional company ID to filter by
     * @return page of users
     */
    public org.springframework.data.domain.Page<User> execute(org.springframework.data.domain.Pageable pageable,
            Long companyId) {
        if (companyId != null) {
            return userRepository.findAllByCompanyId(companyId, pageable);
        }
        return userRepository.findAll(pageable);
    }

    /**
     * Execute the use case to get all users with pagination (legacy support/admin
     * only).
     *
     * @param pageable pagination information
     * @return page of users
     */
    public org.springframework.data.domain.Page<User> execute(org.springframework.data.domain.Pageable pageable) {
        return execute(pageable, null);
    }
}
