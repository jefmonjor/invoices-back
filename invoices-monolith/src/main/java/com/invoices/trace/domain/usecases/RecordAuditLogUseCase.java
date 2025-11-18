package com.invoices.trace.domain.usecases;

import com.invoices.trace.domain.entities.AuditLog;
import com.invoices.trace.domain.events.InvoiceEvent;
import com.invoices.trace.domain.ports.AuditLogRepository;

/**
 * Use Case for recording an audit log from an invoice event.
 * Encapsulates the business logic for creating audit log entries.
 */
public class RecordAuditLogUseCase {

    private final AuditLogRepository auditLogRepository;

    public RecordAuditLogUseCase(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Executes the record audit log use case.
     *
     * @param event         the invoice event
     * @param eventDataJson the event data serialized as JSON
     * @return the created AuditLog entity
     */
    public AuditLog execute(InvoiceEvent event, String eventDataJson) {
        // Create audit log from event
        AuditLog auditLog = new AuditLog(
                event.getEventType(),
                event.getInvoiceId(),
                event.getInvoiceNumber(),
                event.getClientId(),
                event.getClientEmail(),
                event.getTotal(),
                event.getStatus(),
                eventDataJson
        );

        // Save to repository
        return auditLogRepository.save(auditLog);
    }
}
