package com.invoices.trace.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Record representing an invoice event received from Kafka
 */
public record InvoiceEvent(
        String eventType,
        Long invoiceId,
        String invoiceNumber,
        Long clientId,
        String clientEmail,
        BigDecimal total,
        String status,
        LocalDateTime timestamp
) {
}
