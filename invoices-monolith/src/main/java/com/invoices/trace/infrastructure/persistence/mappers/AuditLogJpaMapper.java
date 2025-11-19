package com.invoices.trace.infrastructure.persistence.mappers;

import com.invoices.trace.domain.entities.AuditLog;
import com.invoices.trace.infrastructure.persistence.entities.AuditLogJpaEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between domain AuditLog and JPA AuditLogJpaEntity.
 * This isolates the domain layer from JPA/persistence concerns.
 */
@Component
public class AuditLogJpaMapper {

    /**
     * Converts a domain AuditLog entity to a JPA entity.
     *
     * @param domainAuditLog the domain audit log
     * @return the JPA entity
     */
    public AuditLogJpaEntity toJpaEntity(AuditLog domainAuditLog) {
        if (domainAuditLog == null) {
            return null;
        }

        return AuditLogJpaEntity.builder()
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

    /**
     * Converts a JPA entity to a domain AuditLog entity.
     *
     * @param jpaEntity the JPA entity
     * @return the domain audit log
     */
    public AuditLog toDomainEntity(AuditLogJpaEntity jpaEntity) {
        if (jpaEntity == null) {
            return null;
        }

        return new AuditLog(
                jpaEntity.getId(),
                jpaEntity.getEventType(),
                jpaEntity.getInvoiceId(),
                jpaEntity.getInvoiceNumber(),
                jpaEntity.getClientId(),
                jpaEntity.getClientEmail(),
                jpaEntity.getTotal(),
                jpaEntity.getStatus(),
                jpaEntity.getEventData(),
                jpaEntity.getCreatedAt()
        );
    }
}
