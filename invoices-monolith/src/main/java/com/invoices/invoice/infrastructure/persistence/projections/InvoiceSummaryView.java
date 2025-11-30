package com.invoices.invoice.infrastructure.persistence.projections;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA Projection for Invoice Summary.
 * Fetches only essential fields for list views.
 */
public interface InvoiceSummaryView {
    Long getId();

    String getInvoiceNumber();

    LocalDateTime getIssueDate();

    BigDecimal getTotalAmount();

    String getStatus();

    Long getClientId();

    Long getCompanyId();
}
