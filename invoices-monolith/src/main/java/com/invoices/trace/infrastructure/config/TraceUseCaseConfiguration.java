package com.invoices.trace.infrastructure.config;

import com.invoices.trace.domain.ports.AuditLogRepository;
import com.invoices.trace.domain.usecases.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for Trace use cases.
 * This wires up all use cases with their dependencies (ports).
 */
@Configuration
public class TraceUseCaseConfiguration {

    @Bean
    public RecordAuditLogUseCase recordAuditLogUseCase(
            AuditLogRepository auditLogRepository
    ) {
        return new RecordAuditLogUseCase(auditLogRepository);
    }

    @Bean
    public GetAuditLogByIdUseCase getAuditLogByIdUseCase(
            AuditLogRepository auditLogRepository
    ) {
        return new GetAuditLogByIdUseCase(auditLogRepository);
    }

    @Bean
    public GetAuditLogsByInvoiceUseCase getAuditLogsByInvoiceUseCase(
            AuditLogRepository auditLogRepository
    ) {
        return new GetAuditLogsByInvoiceUseCase(auditLogRepository);
    }

    @Bean
    public GetAuditLogsByClientUseCase getAuditLogsByClientUseCase(
            AuditLogRepository auditLogRepository
    ) {
        return new GetAuditLogsByClientUseCase(auditLogRepository);
    }

    @Bean
    public GetAuditLogsByEventTypeUseCase getAuditLogsByEventTypeUseCase(
            AuditLogRepository auditLogRepository
    ) {
        return new GetAuditLogsByEventTypeUseCase(auditLogRepository);
    }

    @Bean
    public GetAllAuditLogsUseCase getAllAuditLogsUseCase(
            AuditLogRepository auditLogRepository
    ) {
        return new GetAllAuditLogsUseCase(auditLogRepository);
    }
}
