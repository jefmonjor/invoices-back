package com.invoices.trace.domain.usecases;

import com.invoices.trace.domain.entities.AuditLog;
import com.invoices.trace.domain.ports.AuditLogRepository;

import java.util.List;

/**
 * Use Case for retrieving all audit logs for a specific invoice.
 */
public class GetAuditLogsByInvoiceUseCase {

    private final AuditLogRepository auditLogRepository;

    public GetAuditLogsByInvoiceUseCase(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Executes the get audit logs by invoice use case.
     *
     * @param invoiceId the ID of the invoice
     * @return List of audit logs for the invoice, ordered by creation date (newest first)
     */
    public List<AuditLog> execute(Long invoiceId) {
        if (invoiceId == null) {
            throw new IllegalArgumentException("Invoice ID cannot be null");
        }
        return auditLogRepository.findByInvoiceId(invoiceId);
    }
}
