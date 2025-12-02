package com.invoices.verifactu.domain.services;

import com.invoices.invoice.domain.ports.VerifactuMetricsRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Domain Service for analyzing VeriFactu trends.
 *
 * Encapsulates business logic for:
 * - Daily trend calculation
 * - Success rate computation
 * - Trend analysis over time
 */
@Slf4j
public class VerifactuTrendAnalyzer {

    private final VerifactuMetricsRepository metricsRepository;

    public VerifactuTrendAnalyzer(VerifactuMetricsRepository metricsRepository) {
        this.metricsRepository = metricsRepository;
    }

    /**
     * Analyze daily trends for the given number of days.
     *
     * Business logic:
     * 1. Validate number of days (1-365)
     * 2. For each day in range (backwards from today)
     * 3. Calculate daily metrics:
     *    - Total invoices
     *    - Accepted invoices
     *    - Rejected invoices
     *    - Success rate percentage
     * 4. Return trends sorted chronologically (oldest to newest)
     *
     * @param numberOfDays Number of days to analyze (1-365)
     * @return List of daily trends
     * @throws IllegalArgumentException if days out of range
     */
    public List<DailyTrend> analyzeTrends(int numberOfDays) {
        log.debug("Analyzing trends for {} days", numberOfDays);

        if (numberOfDays <= 0 || numberOfDays > 365) {
            throw new IllegalArgumentException(
                    "Days parameter must be between 1 and 365, got: " + numberOfDays);
        }

        List<DailyTrend> trends = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // Iterate backwards from today
        for (int i = 0; i < numberOfDays; i++) {
            LocalDateTime dayStart = now.minusDays(i)
                    .withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime dayEnd = dayStart.plusDays(1);

            // Query metrics for the day
            Long total = metricsRepository.countByCreatedAtBetween(dayStart, dayEnd);
            Long accepted = metricsRepository.countByVerifactuStatusAndCreatedAtBetween(
                    "ACCEPTED", dayStart, dayEnd);
            Long rejected = metricsRepository.countByVerifactuStatusAndCreatedAtBetween(
                    "REJECTED", dayStart, dayEnd);

            // Calculate success rate
            Double successRate = total > 0
                    ? (double) accepted / total * 100
                    : 0.0;

            trends.add(DailyTrend.builder()
                    .date(dayStart.toLocalDate().toString())
                    .total(total)
                    .accepted(accepted)
                    .rejected(rejected)
                    .successRate(Math.round(successRate * 100.0) / 100.0)  // Round to 2 decimals
                    .build());
        }

        // Sort chronologically (oldest to newest)
        trends.sort(Comparator.comparing(DailyTrend::getDate));

        log.debug("Generated {} daily trends", trends.size());
        return trends;
    }

    /**
     * DTO for daily trend information.
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DailyTrend {
        private String date;        // ISO date format (YYYY-MM-DD)
        private Long total;         // Total invoices
        private Long accepted;      // Accepted invoices
        private Long rejected;      // Rejected invoices
        private Double successRate; // Percentage (0-100)
    }
}
