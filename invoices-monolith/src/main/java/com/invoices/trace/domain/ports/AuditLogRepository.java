package com.invoices.trace.domain.ports;

import com.invoices.trace.domain.entities.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Port (interface) for audit log repository operations.
 * This defines the contract for persistence without exposing implementation details.
 * Infrastructure layer will provide the actual implementation.
 */
public interface AuditLogRepository {

    /**
     * Find an audit log by its ID.
     *
     * @param id the audit log ID
     * @return Optional containing the audit log if found
     */
    Optional<AuditLog> findById(Long id);

    /**
     * Find all audit logs for a specific invoice, ordered by creation date (newest first).
     *
     * @param invoiceId the invoice ID
     * @return List of audit logs for the invoice
     */
    List<AuditLog> findByInvoiceId(Long invoiceId);

    /**
     * Find all audit logs for a specific client, ordered by creation date (newest first).
     *
     * @param clientId the client ID
     * @return List of audit logs for the client
     */
    List<AuditLog> findByClientId(Long clientId);

    /**
     * Find all audit logs of a specific event type, ordered by creation date (newest first).
     *
     * @param eventType the event type
     * @return List of audit logs matching the event type
     */
    List<AuditLog> findByEventType(String eventType);

    /**
     * Find all audit logs with pagination support.
     *
     * @param pageable pagination information
     * @return Page of audit logs
     */
    Page<AuditLog> findAll(Pageable pageable);

    /**
     * Save an audit log (create or update).
     *
     * @param auditLog the audit log to save
     * @return the saved audit log with generated ID if new
     */
    AuditLog save(AuditLog auditLog);

    /**
     * Delete an audit log by ID.
     *
     * @param id the audit log ID
     */
    void deleteById(Long id);

    /**
     * Check if an audit log exists by ID.
     *
     * @param id the audit log ID
     * @return true if exists, false otherwise
     */
    boolean existsById(Long id);
}
