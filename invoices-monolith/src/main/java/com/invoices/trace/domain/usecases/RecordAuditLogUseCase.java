
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
        AuditLog auditLog = AuditLog.builder()
                .companyId(event.getCompanyId())
                .eventType(event.getEventType())
                .invoiceId(event.getInvoiceId())
                .invoiceNumber(event.getInvoiceNumber())
                .clientId(event.getClientId())
                .clientEmail(event.getClientEmail())
                .total(event.getTotal())
                .status(event.getStatus())
                .eventData(eventDataJson)
                .createdAt(java.time.LocalDateTime.now())
                .build();

        // Save to repository
        return auditLogRepository.save(auditLog);
    }
}
