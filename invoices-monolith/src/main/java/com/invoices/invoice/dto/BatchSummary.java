package com.invoices.invoice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Summary of VER*FACTU batch processing results.
 * Used for email notifications after batch scheduler runs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchSummary {

    /**
     * Timestamp when batch was executed
     */
    private LocalDateTime timestamp;

    /**
     * Total number of invoices processed
     */
    private int totalProcessed;

    /**
     * Number of successfully requeued invoices
     */
    private int successful;

    /**
     * Number of invoices that failed to requeue
     */
    private int failed;

    /**
     * Number of invoices pending for more than 48 hours (critical)
     */
    private int criticalPending;

    /**
     * Success rate percentage (0-100)
     */
    private double successRate;
}
