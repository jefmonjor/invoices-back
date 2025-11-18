package com.invoices.trace.domain.usecases;

import com.invoices.trace.domain.entities.AuditLog;
import com.invoices.trace.domain.ports.AuditLogRepository;
import com.invoices.trace.exception.AuditLogNotFoundException;

/**
 * Use Case for retrieving an audit log by ID.
 */
public class GetAuditLogByIdUseCase {

    private final AuditLogRepository auditLogRepository;

    public GetAuditLogByIdUseCase(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Executes the get audit log by ID use case.
     *
     * @param id the ID of the audit log to retrieve
     * @return the AuditLog entity
     * @throws AuditLogNotFoundException if audit log not found
     */
    public AuditLog execute(Long id) {
        return auditLogRepository.findById(id)
                .orElseThrow(() -> new AuditLogNotFoundException(id));
    }
}
