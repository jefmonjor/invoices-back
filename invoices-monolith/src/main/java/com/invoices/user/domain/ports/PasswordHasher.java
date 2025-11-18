package com.invoices.user.domain.ports;

/**
 * Port (interface) for password hashing operations.
 * This abstraction allows the domain layer to be independent of
 * specific hashing implementations (BCrypt, Argon2, etc).
 */
public interface PasswordHasher {

    /**
     * Hash a plain text password
     *
     * @param plainPassword the plain text password to hash
     * @return the hashed password
     */
    String hash(String plainPassword);

    /**
     * Verify if a plain password matches a hashed password
     *
     * @param plainPassword the plain text password to verify
     * @param hashedPassword the hashed password to compare against
     * @return true if passwords match, false otherwise
     */
    boolean matches(String plainPassword, String hashedPassword);
}
