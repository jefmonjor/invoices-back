package com.invoices.invoice.domain.ports;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Port for monitoring Dead Letter Queue.
 *
 * Abstracts Redis/message broker access for DLQ monitoring.
 * Keeps domain layer independent of infrastructure details.
 */
public interface DLQMonitorService {

    /**
     * Get count of messages currently in the DLQ.
     *
     * @return Number of messages in DLQ
     */
    Long getDLQCount();

    /**
     * Get batch metrics from monitoring system.
     *
     * @return Batch metrics DTO
     */
    VerifactuBatchMetrics getBatchMetrics();

    /**
     * DTO for batch metrics information.
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    class VerifactuBatchMetrics {
        private Long lastBatchId;
        private String lastBatchTime;
        private Long invoicesProcessed;
        private Long invoicesSuccessful;
        private Long invoicesFailed;
    }
}
