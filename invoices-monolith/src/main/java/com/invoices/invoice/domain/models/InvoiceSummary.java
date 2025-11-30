package com.invoices.invoice.domain.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Domain model for Invoice Summary.
 * Lightweight representation of an invoice for list views.
 */
public record InvoiceSummary(
        Long id,
        String invoiceNumber,
        LocalDateTime issueDate,
        BigDecimal totalAmount,
        String status,
        Long clientId,
        Long companyId) {
}
