package com.invoices.trace.domain.usecases;

import com.invoices.trace.domain.entities.AuditLog;
import com.invoices.trace.domain.ports.AuditLogRepository;

import java.util.List;

import org.springframework.stereotype.Service;

/**
 * Use Case for retrieving audit logs for a specific company.
 */
@Service
public class GetAuditLogsByCompanyUseCase {

    private final AuditLogRepository auditLogRepository;

    public GetAuditLogsByCompanyUseCase(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Executes the use case.
     *
     * @param companyId the company ID
     * @return List of audit logs for the company
     */
    public List<AuditLog> execute(Long companyId) {
        if (companyId == null) {
            throw new IllegalArgumentException("Company ID cannot be null");
        }
        return auditLogRepository.findByCompanyId(companyId);
    }
}
