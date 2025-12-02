package com.invoices.invoice.presentation.controllers;

import com.invoices.invoice.domain.usecases.GetVerifactuMetricsUseCase;
import com.invoices.invoice.domain.usecases.GetVerifactuTrendsUseCase;
import com.invoices.verifactu.domain.services.VerifactuTrendAnalyzer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for VeriFactu metrics and monitoring.
 *
 * Delegates all business logic to domain use cases.
 * Responsible only for:
 * - Extracting HTTP request parameters
 * - Calling appropriate use case
 * - Returning HTTP responses
 *
 * No business logic should be in this controller.
 */
@RestController
@RequestMapping("/api/verifactu/metrics")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        prefix = "verifactu.metrics",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class VerifactuMetricsController {

    // Inject use cases instead of repositories
    private final GetVerifactuMetricsUseCase getMetricsUseCase;
    private final GetVerifactuTrendsUseCase getTrendsUseCase;

    /**
     * GET /api/verifactu/metrics - Get current VeriFactu metrics
     *
     * Returns real-time metrics including:
     * - Invoices verified today
     * - Pending invoices
     * - Dead letter queue count
     * - Success rates (today and 24h)
     * - Top errors
     * - Batch metrics
     *
     * @return VerifactuMetricsDTO with current metrics
     */
    @GetMapping
    public ResponseEntity<GetVerifactuMetricsUseCase.VerifactuMetricsDTO> getMetrics() {
        log.info("GET /api/verifactu/metrics - Fetching VeriFactu metrics");

        try {
            // Execute use case
            GetVerifactuMetricsUseCase.VerifactuMetricsDTO metrics = getMetricsUseCase.execute();

            log.debug("Metrics retrieved: today={}, pending={}, dlq={}",
                    metrics.getTodayVerified(),
                    metrics.getPending(),
                    metrics.getInDLQ());

            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            log.error("Error fetching metrics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/verifactu/metrics/trends - Get daily trends
     *
     * Returns daily trend data for the specified number of days.
     * Each day includes:
     * - Total invoices
     * - Accepted invoices
     * - Rejected invoices
     * - Success rate percentage
     *
     * @param days Number of days to analyze (1-365, default 30)
     * @return List of daily trends
     */
    @GetMapping("/trends")
    public ResponseEntity<List<VerifactuTrendAnalyzer.DailyTrend>> getTrends(
            @RequestParam(defaultValue = "30") int days) {
        log.info("GET /api/verifactu/metrics/trends - Fetching {} days of trends", days);

        try {
            // Execute use case
            List<VerifactuTrendAnalyzer.DailyTrend> trends = getTrendsUseCase.execute(days);

            log.debug("Trends retrieved: {} days", trends.size());
            return ResponseEntity.ok(trends);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid trends request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error fetching trends: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
