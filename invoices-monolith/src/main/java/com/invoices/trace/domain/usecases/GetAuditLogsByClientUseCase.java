package com.invoices.trace.domain.usecases;

import com.invoices.trace.domain.entities.AuditLog;
import com.invoices.trace.domain.ports.AuditLogRepository;

import java.util.List;

/**
 * Use Case for retrieving all audit logs for a specific client.
 */
public class GetAuditLogsByClientUseCase {

    private final AuditLogRepository auditLogRepository;

    public GetAuditLogsByClientUseCase(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Executes the get audit logs by client use case.
     *
     * @param clientId the ID of the client
     * @return List of audit logs for the client, ordered by creation date (newest first)
     */
    public List<AuditLog> execute(Long clientId) {
        if (clientId == null) {
            throw new IllegalArgumentException("Client ID cannot be null");
        }
        return auditLogRepository.findByClientId(clientId);
    }
}
