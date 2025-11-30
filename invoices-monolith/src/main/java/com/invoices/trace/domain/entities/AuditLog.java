package com.invoices.trace.domain.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Domain entity representing an audit log entry (Clean Architecture - Pure
 * POJO).
 * This entity contains only business logic and has no framework dependencies.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    private Long id;
    private Long companyId;
    private String eventType;
    private Long invoiceId;
    private String invoiceNumber;
    private Long clientId;
    private String clientEmail;
    private BigDecimal total;
    private String status;
    private String eventData;
    private LocalDateTime createdAt;

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
        if (createdAt == null)
            return 0;
        return java.time.temporal.ChronoUnit.DAYS.between(createdAt, LocalDateTime.now());
    }

    /**
     * Checks if this audit log is older than a specified number of days.
     */
    public boolean isOlderThan(int days) {
        return getAgeInDays() > days;
    }
}
