package com.invoices.trace.infrastructure.persistence.repositories;

import com.invoices.trace.infrastructure.persistence.entities.AuditLogJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data JPA repository for AuditLogJpaEntity.
 * This interface provides CRUD operations and custom queries for audit log
 * persistence.
 * No @Repository needed - Spring Data JPA auto-detects this interface.
 */
public interface JpaAuditLogRepository extends JpaRepository<AuditLogJpaEntity, Long> {

    /**
     * Find all audit logs for a specific invoice, ordered by creation date (newest
     * first).
     *
     * @param invoiceId the invoice ID
     * @return List of audit logs for the invoice
     */
    List<AuditLogJpaEntity> findByInvoiceIdOrderByCreatedAtDesc(Long invoiceId);

    /**
     * Find all audit logs for a specific client, ordered by creation date (newest
     * first).
     *
     * @param clientId the client ID
     * @return List of audit logs for the client
     */
    List<AuditLogJpaEntity> findByClientIdOrderByCreatedAtDesc(Long clientId);

    /**
     * Find all audit logs for a specific company, ordered by creation date (newest
     * first).
     *
     * @param companyId the company ID
     * @return List of audit logs for the company
     */
    List<AuditLogJpaEntity> findByCompanyIdOrderByCreatedAtDesc(Long companyId);

    /**
     * Find all audit logs of a specific event type, ordered by creation date
     * (newest first).
     *
     * @param eventType the event type
     * @return List of audit logs matching the event type
     */
    List<AuditLogJpaEntity> findByEventTypeOrderByCreatedAtDesc(String eventType);
}
