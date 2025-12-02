package com.invoices.invoice.domain.usecases;

import com.invoices.invoice.domain.ports.DLQMonitorService;
import com.invoices.invoice.domain.ports.VerifactuMetricsRepository;
import com.invoices.verifactu.domain.services.VerifactuErrorAnalyzer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;

/**
 * UseCase for retrieving VeriFactu metrics.
 *
 * Encapsulates business logic for metrics aggregation:
 * - Real-time counters
 * - Success rates
 * - Error analysis
 * - Batch metrics
 * - Performance metrics
 */
@Slf4j
public class GetVerifactuMetricsUseCase {

    private final VerifactuMetricsRepository metricsRepository;
    private final DLQMonitorService dlqMonitor;
    private final VerifactuErrorAnalyzer errorAnalyzer;

    public GetVerifactuMetricsUseCase(
            VerifactuMetricsRepository metricsRepository,
            DLQMonitorService dlqMonitor,
            VerifactuErrorAnalyzer errorAnalyzer) {
        this.metricsRepository = metricsRepository;
        this.dlqMonitor = dlqMonitor;
        this.errorAnalyzer = errorAnalyzer;
    }

    /**
     * Get current VeriFactu metrics.
     *
     * Orchestrates multiple metrics queries:
     * 1. Real-time counters (today, pending, DLQ)
     * 2. Success rates (today, 24h)
     * 3. Error analysis (top errors)
     * 4. Batch metrics
     * 5. Overall processing time
     *
     * @return VerifactuMetricsDTO with all metrics
     */
    public VerifactuMetricsDTO execute() {
        log.debug("Fetching VeriFactu metrics");

        LocalDateTime today = LocalDateTime.now()
                .withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime yesterday = today.minusDays(1);

        // Real-time counters
        Long todayVerified = metricsRepository.countByVerifactuStatusAndCreatedAtAfter(
                "ACCEPTED", today);
        Long pending = metricsRepository.countByVerifactuStatusIn(
                java.util.List.of("PENDING", "PROCESSING"));
        Long inDLQ = dlqMonitor.getDLQCount();

        // Success rates
        Long totalToday = metricsRepository.countByCreatedAtAfter(today);
        Double successRateToday = totalToday > 0
                ? Math.round((double) todayVerified / totalToday * 100 * 100.0) / 100.0
                : 0.0;

        Long total24h = metricsRepository.countByCreatedAtAfter(yesterday);
        Long accepted24h = metricsRepository.countByVerifactuStatusAndCreatedAtAfter(
                "ACCEPTED", yesterday);
        Double successRate24h = total24h > 0
                ? Math.round((double) accepted24h / total24h * 100 * 100.0) / 100.0
                : 0.0;

        // Error analysis
        List<VerifactuErrorAnalyzer.ErrorCount> topErrors = errorAnalyzer.analyzeErrors(
                yesterday, LocalDateTime.now());

        // Batch metrics
        DLQMonitorService.VerifactuBatchMetrics batchMetrics = dlqMonitor.getBatchMetrics();

        // Average processing time (would be calculated from actual data)
        Double avgProcessingTime = 3.5;

        log.debug("VeriFactu metrics: today={}, pending={}, dlq={}, successRate={}%",
                todayVerified, pending, inDLQ, successRateToday);

        return VerifactuMetricsDTO.builder()
                .todayVerified(todayVerified)
                .pending(pending)
                .inDLQ(inDLQ)
                .successRate(successRateToday)
                .last24hSuccessRate(successRate24h)
                .avgProcessingTimeSeconds(avgProcessingTime)
                .totalProcessed(metricsRepository.countAll())
                .topErrors(topErrors)
                .batchMetrics(batchMetrics)
                .build();
    }

    /**
     * DTO for VeriFactu metrics response.
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class VerifactuMetricsDTO {
        private Long todayVerified;                  // Verified today
        private Long pending;                        // Currently pending
        private Long inDLQ;                          // In dead letter queue
        private Double successRate;                  // Today's success rate %
        private Double last24hSuccessRate;           // Last 24h success rate %
        private Double avgProcessingTimeSeconds;     // Average processing time
        private Long totalProcessed;                 // Total all-time processed
        private List<VerifactuErrorAnalyzer.ErrorCount> topErrors;           // Top errors
        private DLQMonitorService.VerifactuBatchMetrics batchMetrics;        // Batch info
    }
}
