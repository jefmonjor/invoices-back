package com.invoices.audit.infrastructure.persistence.adapters;

import com.invoices.audit.domain.entities.PlatformAdminAuditLog;
import com.invoices.audit.domain.ports.PlatformAdminAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PlatformAdminAuditLogRepositoryAdapter implements PlatformAdminAuditLogRepository {

    private final com.invoices.audit.infrastructure.persistence.repositories.PlatformAdminAuditLogRepository jpaRepository;

    @Override
    public PlatformAdminAuditLog save(PlatformAdminAuditLog auditLog) {
        return jpaRepository.save(auditLog);
    }
}
