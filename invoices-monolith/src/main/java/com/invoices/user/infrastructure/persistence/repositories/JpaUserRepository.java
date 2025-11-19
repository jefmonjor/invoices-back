package com.invoices.user.infrastructure.persistence.repositories;

import com.invoices.user.infrastructure.persistence.entities.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA repository for UserJpaEntity.
 * This interface provides CRUD operations and custom queries for user persistence.
 * No @Repository needed - Spring Data JPA auto-detects this interface.
 */
public interface JpaUserRepository extends JpaRepository<UserJpaEntity, Long> {

    /**
     * Find a user by email
     *
     * @param email the email to search
     * @return Optional containing the user if found
     */
    Optional<UserJpaEntity> findByEmail(String email);

    /**
     * Check if a user exists with the given email
     *
     * @param email the email to check
     * @return true if exists, false otherwise
     */
    boolean existsByEmail(String email);
}
