package com.invoices.user.domain.ports;

import com.invoices.user.domain.entities.User;

import java.util.List;
import java.util.Optional;

/**
 * Port (interface) for User repository operations.
 * This is a domain interface that will be implemented by infrastructure layer.
 * Following the Dependency Inversion Principle of Clean Architecture.
 */
public interface UserRepository {

    /**
     * Find a user by their ID
     *
     * @param id the user ID
     * @return Optional containing the user if found
     */
    Optional<User> findById(Long id);

    /**
     * Find a user by their email address
     *
     * @param email the user email
     * @return Optional containing the user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Find all users in the system
     *
     * @return list of all users
     */
    List<User> findAll();

    /**
     * Find all users with pagination
     *
     * @param pageable pagination information
     * @return page of users
     */
    org.springframework.data.domain.Page<User> findAll(org.springframework.data.domain.Pageable pageable);

    /**
     * Find all users with pagination filtered by company
     *
     * @param companyId the company ID
     * @param pageable  pagination information
     * @return page of users
     */
    org.springframework.data.domain.Page<User> findAllByCompanyId(Long companyId,
            org.springframework.data.domain.Pageable pageable);

    /**
     * Save a user (create or update)
     *
     * @param user the user to save
     * @return the saved user with generated ID if new
     */
    User save(User user);

    /**
     * Delete a user by their ID
     *
     * @param id the user ID to delete
     */
    void deleteById(Long id);

    /**
     * Check if a user exists with the given email
     *
     * @param email the email to check
     * @return true if exists, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Check if a user exists with the given ID
     *
     * @param id the user ID to check
     * @return true if exists, false otherwise
     */
    boolean existsById(Long id);
}
