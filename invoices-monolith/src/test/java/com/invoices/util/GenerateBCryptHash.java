package com.invoices.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utility to generate BCrypt hashes for testing
 */
public class GenerateBCryptHash {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
        String password = "admin123";
        String hash = encoder.encode(password);

        System.out.println("Password: " + password);
        System.out.println("BCrypt Hash (strength 12): " + hash);

        // Verify it matches
        boolean matches = encoder.matches(password, hash);
        System.out.println("Verification: " + matches);

        // Test old hash
        String oldHash = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
        boolean oldMatches = encoder.matches(password, oldHash);
        System.out.println("\nOld hash from V1 migration: " + oldHash);
        System.out.println("Old hash matches with BCrypt(12): " + oldMatches);
    }
}
