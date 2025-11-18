package com.invoices.trace.domain.usecases;

import com.invoices.trace.domain.entities.AuditLog;
import com.invoices.trace.domain.ports.AuditLogRepository;

import java.util.List;

/**
 * Use Case for retrieving all audit logs of a specific event type.
 */
public class GetAuditLogsByEventTypeUseCase {

    private final AuditLogRepository auditLogRepository;

    public GetAuditLogsByEventTypeUseCase(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Executes the get audit logs by event type use case.
     *
     * @param eventType the event type
     * @return List of audit logs matching the event type, ordered by creation date (newest first)
     */
    public List<AuditLog> execute(String eventType) {
        if (eventType == null || eventType.trim().isEmpty()) {
            throw new IllegalArgumentException("Event type cannot be null or empty");
        }
        return auditLogRepository.findByEventType(eventType);
    }
}
