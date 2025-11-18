package com.invoices.user.infrastructure.security;

import com.invoices.user.domain.ports.PasswordHasher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Implementation of PasswordHasher port using BCrypt.
 * This adapter connects the domain layer with Spring Security's password encoder.
 */
@Component
public class BcryptPasswordHasher implements PasswordHasher {

    private final PasswordEncoder passwordEncoder;

    public BcryptPasswordHasher(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String hash(String plainPassword) {
        return passwordEncoder.encode(plainPassword);
    }

    @Override
    public boolean matches(String plainPassword, String hashedPassword) {
        return passwordEncoder.matches(plainPassword, hashedPassword);
    }
}
