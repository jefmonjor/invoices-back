package com.invoices.security.domain.ports;

import com.invoices.security.domain.entities.RefreshToken;

import java.util.Optional;

/**
 * Port for accessing RefreshToken data.
 * This is a domain interface that will be implemented by infrastructure layer.
 * Keeps domain layer independent of database implementation.
 */
public interface RefreshTokenRepository {

    /**
     * Find a refresh token by its token string.
     *
     * @param token the token string
     * @return Optional containing the refresh token if found
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Save a refresh token.
     *
     * @param token the token to save
     * @return the saved token
     */
    RefreshToken save(RefreshToken token);

    /**
     * Delete a refresh token.
     *
     * @param token the token to delete
     */
    void delete(RefreshToken token);

    /**
     * Delete all refresh tokens for a user.
     *
     * @param userId the user ID
     * @return number of tokens deleted
     */
    int deleteByUserId(Long userId);
}
