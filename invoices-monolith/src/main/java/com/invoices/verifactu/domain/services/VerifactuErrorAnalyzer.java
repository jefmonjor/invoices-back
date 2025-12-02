package com.invoices.verifactu.domain.services;

import com.invoices.invoice.domain.ports.VerifactuMetricsRepository;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Domain Service for analyzing VeriFactu errors.
 *
 * Encapsulates business logic for:
 * - Error distribution analysis
 * - Top error identification
 * - Error percentage calculation
 */
@Slf4j
public class VerifactuErrorAnalyzer {

    private final VerifactuMetricsRepository metricsRepository;

    public VerifactuErrorAnalyzer(VerifactuMetricsRepository metricsRepository) {
        this.metricsRepository = metricsRepository;
    }

    /**
     * Analyze error distribution for the given period.
     *
     * Business logic:
     * 1. Get counts for each error type
     * 2. Calculate percentages
     * 3. Sort by frequency (descending)
     * 4. Return top 5 errors
     *
     * @param from Start date
     * @param to End date
     * @return List of errors sorted by percentage (highest first)
     */
    public List<ErrorCount> analyzeErrors(LocalDateTime from, LocalDateTime to) {
        log.debug("Analyzing errors from {} to {}", from, to);

        Map<String, Long> errorDistribution = metricsRepository.getErrorDistribution(from, to);

        if (errorDistribution.isEmpty()) {
            log.debug("No errors found in the period");
            return Collections.emptyList();
        }

        Long totalErrors = errorDistribution.values().stream()
                .mapToLong(Long::longValue)
                .sum();

        if (totalErrors == 0) {
            return Collections.emptyList();
        }

        List<ErrorCount> errors = errorDistribution.entrySet().stream()
                .map(entry -> ErrorCount.builder()
                        .errorType(mapErrorType(entry.getKey()))
                        .count(entry.getValue())
                        .percentage((double) entry.getValue() / totalErrors * 100)
                        .build())
                .sorted(Comparator.comparingDouble(ErrorCount::getPercentage).reversed())
                .limit(5)  // Top 5 errors
                .collect(Collectors.toList());

        log.debug("Found {} types of errors in the period", errors.size());
        return errors;
    }

    /**
     * Maps error code to user-friendly message.
     */
    private String mapErrorType(String code) {
        return switch(code) {
            case "REJECTED" -> "Rechazado por AEAT";
            case "TIMEOUT" -> "Timeout en procesamiento";
            case "FAILED" -> "Error de procesamiento";
            case "PENDING" -> "Pendiente";
            case "PROCESSING" -> "Procesando";
            default -> code;
        };
    }

    /**
     * DTO for error count information.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ErrorCount {
        private String errorType;
        private Long count;
        private Double percentage;
    }
}
