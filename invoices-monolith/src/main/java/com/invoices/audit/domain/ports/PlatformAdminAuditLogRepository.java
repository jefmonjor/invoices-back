package com.invoices.audit.domain.ports;

import com.invoices.audit.domain.entities.PlatformAdminAuditLog;

/**
 * Port for accessing PlatformAdminAuditLog data.
 * This is a domain interface that will be implemented by infrastructure layer.
 * Keeps domain layer independent of database implementation.
 */
public interface PlatformAdminAuditLogRepository {

    /**
     * Save an audit log entry.
     *
     * @param auditLog the audit log to save
     * @return the saved audit log
     */
    PlatformAdminAuditLog save(PlatformAdminAuditLog auditLog);
}
