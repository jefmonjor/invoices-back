package com.invoices.trace.domain.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain entity representing an audit log entry (Clean Architecture - Pure POJO).
 * This entity contains only business logic and has no framework dependencies.
 */
public class AuditLog {

    private final Long id;
    private final String eventType;
    private final Long invoiceId;
    private final String invoiceNumber;
    private final Long clientId;
    private final String clientEmail;
    private final BigDecimal total;
    private final String status;
    private final String eventData;
    private final LocalDateTime createdAt;

    /**
     * Full constructor for creating an AuditLog domain entity.
     */
    public AuditLog(
            Long id,
            String eventType,
            Long invoiceId,
            String invoiceNumber,
            Long clientId,
            String clientEmail,
            BigDecimal total,
            String status,
            String eventData,
            LocalDateTime createdAt
    ) {
        validateRequiredFields(eventType);

        this.id = id;
        this.eventType = eventType;
        this.invoiceId = invoiceId;
        this.invoiceNumber = invoiceNumber;
        this.clientId = clientId;
        this.clientEmail = clientEmail;
        this.total = total;
        this.status = status;
        this.eventData = eventData;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    /**
     * Constructor for new audit logs (before persistence - no ID).
     */
    public AuditLog(
            String eventType,
            Long invoiceId,
            String invoiceNumber,
            Long clientId,
            String clientEmail,
            BigDecimal total,
            String status,
            String eventData
    ) {
        this(null, eventType, invoiceId, invoiceNumber, clientId, clientEmail,
             total, status, eventData, LocalDateTime.now());
    }

    /**
     * Validates required fields for the audit log.
     */
    private void validateRequiredFields(String eventType) {
        if (eventType == null || eventType.trim().isEmpty()) {
            throw new IllegalArgumentException("Event type cannot be null or empty");
        }
    }

    // Business methods

    /**
     * Checks if this audit log is related to an invoice.
     */
    public boolean hasInvoice() {
        return invoiceId != null;
    }

    /**
     * Checks if this audit log is related to a client.
     */
    public boolean hasClient() {
        return clientId != null;
    }

    /**
     * Checks if this audit log contains event data (JSON payload).
     */
    public boolean hasEventData() {
        return eventData != null && !eventData.trim().isEmpty();
    }

    /**
     * Checks if the event is of a specific type.
     */
    public boolean isEventType(String type) {
        return eventType.equalsIgnoreCase(type);
    }

    /**
     * Gets the age of this audit log in days.
     */
    public long getAgeInDays() {
        return java.time.temporal.ChronoUnit.DAYS.between(createdAt, LocalDateTime.now());
    }

    /**
     * Checks if this audit log is older than a specified number of days.
     */
    public boolean isOlderThan(int days) {
        return getAgeInDays() > days;
    }

    // Getters

    public Long getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public Long getInvoiceId() {
        return invoiceId;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public Long getClientId() {
        return clientId;
    }

    public String getClientEmail() {
        return clientEmail;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public String getStatus() {
        return status;
    }

    public String getEventData() {
        return eventData;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuditLog auditLog = (AuditLog) o;
        return Objects.equals(id, auditLog.id) &&
               Objects.equals(eventType, auditLog.eventType) &&
               Objects.equals(invoiceId, auditLog.invoiceId) &&
               Objects.equals(createdAt, auditLog.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, eventType, invoiceId, createdAt);
    }

    @Override
    public String toString() {
        return "AuditLog{" +
                "id=" + id +
                ", eventType='" + eventType + '\'' +
                ", invoiceId=" + invoiceId +
                ", invoiceNumber='" + invoiceNumber + '\'' +
                ", clientId=" + clientId +
                ", clientEmail='" + clientEmail + '\'' +
                ", total=" + total +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
