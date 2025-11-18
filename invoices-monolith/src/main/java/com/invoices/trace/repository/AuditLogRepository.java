package com.invoices.trace.repository;

import com.invoices.trace.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByInvoiceIdOrderByCreatedAtDesc(Long invoiceId);

    List<AuditLog> findByClientIdOrderByCreatedAtDesc(Long clientId);

    List<AuditLog> findByEventTypeOrderByCreatedAtDesc(String eventType);
}
