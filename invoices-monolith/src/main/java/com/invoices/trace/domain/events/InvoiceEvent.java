package com.invoices.trace.domain.events;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain event representing an invoice-related event.
 * This is an immutable value object used for event-driven communication.
 */
public class InvoiceEvent {

    private final String eventType;
    private final Long invoiceId;
    private final String invoiceNumber;
    private final Long clientId;
    private final String clientEmail;
    private final BigDecimal total;
    private final String status;
    private final LocalDateTime timestamp;

    public InvoiceEvent(
            String eventType,
            Long invoiceId,
            String invoiceNumber,
            Long clientId,
            String clientEmail,
            BigDecimal total,
            String status,
            LocalDateTime timestamp
    ) {
        if (eventType == null || eventType.trim().isEmpty()) {
            throw new IllegalArgumentException("Event type cannot be null or empty");
        }

        this.eventType = eventType;
        this.invoiceId = invoiceId;
        this.invoiceNumber = invoiceNumber;
        this.clientId = clientId;
        this.clientEmail = clientEmail;
        this.total = total;
        this.status = status;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
    }

    // Business methods

    public boolean isCreatedEvent() {
        return "INVOICE_CREATED".equalsIgnoreCase(eventType);
    }

    public boolean isUpdatedEvent() {
        return "INVOICE_UPDATED".equalsIgnoreCase(eventType);
    }

    public boolean isDeletedEvent() {
        return "INVOICE_DELETED".equalsIgnoreCase(eventType);
    }

    public boolean isPaidEvent() {
        return "INVOICE_PAID".equalsIgnoreCase(eventType);
    }

    // Getters

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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InvoiceEvent that = (InvoiceEvent) o;
        return Objects.equals(eventType, that.eventType) &&
               Objects.equals(invoiceId, that.invoiceId) &&
               Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventType, invoiceId, timestamp);
    }

    @Override
    public String toString() {
        return "InvoiceEvent{" +
                "eventType='" + eventType + '\'' +
                ", invoiceId=" + invoiceId +
                ", invoiceNumber='" + invoiceNumber + '\'' +
                ", clientId=" + clientId +
                ", clientEmail='" + clientEmail + '\'' +
                ", total=" + total +
                ", status='" + status + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
