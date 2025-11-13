package com.invoices.user_service.repository;

import com.invoices.user_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for User entity.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find a user by email address.
     *
     * @param email the email to search for
     * @return an Optional containing the user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if a user exists with the given email.
     *
     * @param email the email to check
     * @return true if a user exists with this email
     */
    boolean existsByEmail(String email);
}
