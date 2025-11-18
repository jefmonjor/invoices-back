package com.invoices.trace.presentation.mappers;

import com.invoices.trace.domain.entities.AuditLog;
import com.invoices.trace.presentation.dto.AuditLogDTO;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between domain AuditLog and presentation DTOs.
 * This isolates the domain layer from presentation concerns.
 */
@Component
public class AuditLogDtoMapper {

    /**
     * Converts a domain AuditLog entity to an AuditLogDTO.
     *
     * @param domainAuditLog the domain audit log
     * @return the AuditLogDTO
     */
    public AuditLogDTO toDTO(AuditLog domainAuditLog) {
        if (domainAuditLog == null) {
            return null;
        }

        return AuditLogDTO.builder()
                .id(domainAuditLog.getId())
                .eventType(domainAuditLog.getEventType())
                .invoiceId(domainAuditLog.getInvoiceId())
                .invoiceNumber(domainAuditLog.getInvoiceNumber())
                .clientId(domainAuditLog.getClientId())
                .clientEmail(domainAuditLog.getClientEmail())
                .total(domainAuditLog.getTotal())
                .status(domainAuditLog.getStatus())
                .eventData(domainAuditLog.getEventData())
                .createdAt(domainAuditLog.getCreatedAt())
                .build();
    }
}
