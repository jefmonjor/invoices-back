package com.invoices.user.infrastructure.security;

import com.invoices.user.domain.ports.PasswordHasher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Implementation of PasswordHasher port using BCrypt.
 * This adapter connects the domain layer with Spring Security's password encoder.
 */
@Slf4j
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
        log.debug("BCrypt matching - Plain password length: {}, Hash prefix: {}",
                plainPassword != null ? plainPassword.length() : 0,
                hashedPassword != null && hashedPassword.length() > 10 ? hashedPassword.substring(0, 10) : "invalid");

        boolean result = passwordEncoder.matches(plainPassword, hashedPassword);

        log.debug("BCrypt match result: {}", result);
        return result;
    }
}
