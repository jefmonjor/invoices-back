package com.invoices.metrics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for company metrics and statistics.
 * Provides operational visibility into company usage.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyMetrics {

    private Long companyId;
    private String companyName;

    // Invoice metrics
    private Long invoiceCount;
    private Long draftInvoiceCount;
    private Long finalizedInvoiceCount;

    // Client metrics
    private Long clientCount;

    // User metrics
    private Long userCount;
    private Long adminCount;

    // Timestamp
    private java.time.Instant generatedAt;
}
