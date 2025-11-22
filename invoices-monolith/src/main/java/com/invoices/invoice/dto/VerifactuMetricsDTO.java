package com.invoices.invoice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for VeriFactu metrics dashboard
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifactuMetricsDTO {

    // Real-time counters
    private Long todayVerified;
    private Long pending;
    private Long inDLQ;

    // Success metrics
    private Double successRate;
    private Double last24hSuccessRate;

    // Performance metrics
    private Double avgProcessingTimeSeconds;
    private Long totalProcessed;

    // Error analysis
    private List<ErrorCount> topErrors;

    // Time distribution
    private Map<Integer, Long> hourlyDistribution; // hora -> count

    // Batch metrics
    private BatchMetrics batchMetrics;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorCount {
        private String errorType;
        private Long count;
        private Double percentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchMetrics {
        private LocalDateTime lastBatchRun;
        private Long lastBatchFound;
        private Long lastBatchRequeued;
        private Long totalBatchRuns;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyTrend {
        private String date;
        private Long total;
        private Long accepted;
        private Long rejected;
        private Double successRate;
    }
}
