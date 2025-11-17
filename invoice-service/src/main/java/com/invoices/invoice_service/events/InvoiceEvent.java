package com.invoices.invoice_service.events;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Evento que se publica en Redis Streams cuando ocurre una operación sobre una factura
 */
public record InvoiceEvent(
        String eventType,       // CREATED, UPDATED, PAID, CANCELLED
        Long invoiceId,
        String invoiceNumber,
        Long clientId,
        String clientEmail,
        BigDecimal total,
        String status,
        LocalDateTime timestamp
) {
}
