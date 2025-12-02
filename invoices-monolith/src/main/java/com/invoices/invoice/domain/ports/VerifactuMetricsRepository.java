package com.invoices.invoice.domain.ports;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Port for VeriFactu metrics queries.
 *
 * Specialized repository for metrics calculations.
 * Avoids complex queries in domain layer.
 * Implementations should use native queries or aggregations for performance.
 */
public interface VerifactuMetricsRepository {

    /**
     * Count invoices by VeriFactu status after a specific date.
     *
     * @param status VeriFactu status (e.g., "ACCEPTED", "REJECTED")
     * @param after Date threshold
     * @return Count of invoices matching criteria
     */
    Long countByVerifactuStatusAndCreatedAtAfter(String status, LocalDateTime after);

    /**
     * Count invoices by VeriFactu status in a list.
     *
     * @param statuses List of statuses to match
     * @return Count of invoices with any of the statuses
     */
    Long countByVerifactuStatusIn(java.util.List<String> statuses);

    /**
     * Count invoices created after a specific date.
     *
     * @param after Date threshold
     * @return Count of invoices created after date
     */
    Long countByCreatedAtAfter(LocalDateTime after);

    /**
     * Count invoices created between two dates.
     *
     * @param from Start date (inclusive)
     * @param to End date (exclusive)
     * @return Count of invoices in date range
     */
    Long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    /**
     * Count invoices by status and date range.
     *
     * @param status VeriFactu status
     * @param from Start date (inclusive)
     * @param to End date (exclusive)
     * @return Count of invoices matching criteria
     */
    Long countByVerifactuStatusAndCreatedAtBetween(String status, LocalDateTime from, LocalDateTime to);

    /**
     * Get total count of all invoices.
     *
     * @return Total count of invoices
     */
    Long countAll();

    /**
     * Get error distribution for invoices in a date range.
     *
     * Returns a map where:
     * - Key: error status (e.g., "REJECTED", "FAILED", "TIMEOUT")
     * - Value: count of invoices with that error
     *
     * @param from Start date (inclusive)
     * @param to End date (exclusive)
     * @return Map of error type to count
     */
    Map<String, Long> getErrorDistribution(LocalDateTime from, LocalDateTime to);
}
