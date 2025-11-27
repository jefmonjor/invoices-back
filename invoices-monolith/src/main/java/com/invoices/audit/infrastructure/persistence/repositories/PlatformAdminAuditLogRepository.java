package com.invoices.audit.infrastructure.persistence.repositories;

import com.invoices.audit.domain.entities.PlatformAdminAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlatformAdminAuditLogRepository extends JpaRepository<PlatformAdminAuditLog, Long> {
    List<PlatformAdminAuditLog> findByAdminEmailOrderByCreatedAtDesc(String adminEmail);

    List<PlatformAdminAuditLog> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(String targetType, String targetId);
}
