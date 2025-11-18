package com.invoices.trace.infrastructure.persistence.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA Entity for audit log persistence.
 * This is the infrastructure representation of an AuditLog,
 * separate from the domain entity to maintain clean architecture.
 */
@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_log_invoice_id", columnList = "invoice_id"),
        @Index(name = "idx_audit_log_client_id", columnList = "client_id"),
        @Index(name = "idx_audit_log_event_type", columnList = "event_type"),
        @Index(name = "idx_audit_log_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "invoice_id")
    private Long invoiceId;

    @Column(name = "invoice_number", length = 50)
    private String invoiceNumber;

    @Column(name = "client_id")
    private Long clientId;

    @Column(name = "client_email", length = 255)
    private String clientEmail;

    @Column(precision = 10, scale = 2)
    private BigDecimal total;

    @Column(length = 20)
    private String status;

    @Column(name = "event_data", columnDefinition = "TEXT")
    private String eventData;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
