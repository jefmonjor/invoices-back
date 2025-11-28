package com.invoices.auth.infrastructure.persistence.repositories;

import com.invoices.auth.domain.entities.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for password reset tokens.
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    /**
     * Find a valid (not used, not expired) token.
     *
     * @param token the token UUID
     * @return optional containing the token if found and valid
     */
    @Query("SELECT t FROM PasswordResetToken t WHERE t.token = :token AND t.used = false AND t.expiresAt > CURRENT_TIMESTAMP")
    Optional<PasswordResetToken> findValidToken(@Param("token") UUID token);

    /**
     * Find the most recent token for a user.
     *
     * @param userId the user ID
     * @return optional containing the most recent token
     */
    Optional<PasswordResetToken> findTopByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Delete all expired tokens (cleanup job).
     *
     * @param now current timestamp
     * @return number of tokens deleted
     */
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiresAt < :now OR t.used = true")
    int deleteExpiredAndUsedTokens(@Param("now") LocalDateTime now);

    /**
     * Delete all tokens for a specific user.
     *
     * @param userId the user ID
     */
    void deleteAllByUserId(Long userId);
}
