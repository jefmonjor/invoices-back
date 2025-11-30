package com.invoices.security.application.services;

import com.invoices.security.domain.entities.RefreshToken;
import com.invoices.security.infrastructure.persistence.RefreshTokenRepository;

import com.invoices.user.domain.ports.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    @Value("${jwt.refresh-expiration-ms:2592000000}") // 30 days default
    private Long refreshTokenDurationMs;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    public RefreshToken findByToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Refresh token was expired. Please make a new signin request");
        }
        return token;
    }

    @Transactional
    public RefreshToken createRefreshToken(Long userId) {
        RefreshToken refreshToken = new RefreshToken();

        userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        refreshToken.setUserId(userId);
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setRevoked(false);

        // Invalidate previous tokens for this user if needed, or allow multiple
        // sessions
        // For now, we keep it simple and allow multiple sessions or handle cleanup
        // elsewhere

        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public int deleteByUserId(Long userId) {
        userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        return refreshTokenRepository.deleteByUserId(userId);
    }
}
