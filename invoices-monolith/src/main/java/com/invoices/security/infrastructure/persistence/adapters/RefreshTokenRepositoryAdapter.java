package com.invoices.security.infrastructure.persistence.adapters;

import com.invoices.security.domain.entities.RefreshToken;
import com.invoices.security.domain.ports.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final com.invoices.security.infrastructure.persistence.RefreshTokenRepository jpaRepository;

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return jpaRepository.findByToken(token);
    }

    @Override
    public RefreshToken save(RefreshToken token) {
        return jpaRepository.save(token);
    }

    @Override
    public void delete(RefreshToken token) {
        jpaRepository.delete(token);
    }

    @Override
    public int deleteByUserId(Long userId) {
        return jpaRepository.deleteByUserId(userId);
    }
}
