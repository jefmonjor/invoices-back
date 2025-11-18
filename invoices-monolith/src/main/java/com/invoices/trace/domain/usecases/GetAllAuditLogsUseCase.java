package com.invoices.trace.domain.usecases;

import com.invoices.trace.domain.entities.AuditLog;
import com.invoices.trace.domain.ports.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Use Case for retrieving all audit logs with pagination support.
 */
public class GetAllAuditLogsUseCase {

    private final AuditLogRepository auditLogRepository;

    public GetAllAuditLogsUseCase(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Executes the get all audit logs use case with pagination.
     *
     * @param pageable pagination information (page number, size, sort)
     * @return Page of audit logs
     */
    public Page<AuditLog> execute(Pageable pageable) {
        if (pageable == null) {
            throw new IllegalArgumentException("Pageable cannot be null");
        }
        return auditLogRepository.findAll(pageable);
    }
}
