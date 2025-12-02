package com.invoices.auth.domain.ports;

import com.invoices.auth.domain.entities.PasswordResetToken;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository {
    Optional<PasswordResetToken> findValidToken(UUID token);

    int deleteExpiredAndUsedTokens(LocalDateTime now);

    void deleteAllByUserId(Long userId);

    PasswordResetToken save(PasswordResetToken token);
}
