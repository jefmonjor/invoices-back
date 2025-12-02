package com.invoices.invoice.domain.usecases;

import com.invoices.verifactu.domain.services.VerifactuTrendAnalyzer;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * UseCase for retrieving VeriFactu trends over time.
 *
 * Encapsulates business logic for:
 * - Daily trend calculation
 * - Trend analysis and aggregation
 * - Data validation
 */
@Slf4j
public class GetVerifactuTrendsUseCase {

    private final VerifactuTrendAnalyzer trendAnalyzer;

    public GetVerifactuTrendsUseCase(VerifactuTrendAnalyzer trendAnalyzer) {
        this.trendAnalyzer = trendAnalyzer;
    }

    /**
     * Get daily trends for VeriFactu metrics.
     *
     * Business logic:
     * 1. Validate number of days (1-365)
     * 2. Analyze trends using TrendAnalyzer service
     * 3. Return chronologically sorted results (oldest to newest)
     *
     * @param numberOfDays Number of days to analyze (1-365)
     * @return List of daily trends
     * @throws IllegalArgumentException if days out of range
     */
    public List<VerifactuTrendAnalyzer.DailyTrend> execute(int numberOfDays) {
        log.debug("Getting trends for {} days", numberOfDays);

        if (numberOfDays <= 0 || numberOfDays > 365) {
            throw new IllegalArgumentException(
                    "Days parameter must be between 1 and 365, got: " + numberOfDays);
        }

        List<VerifactuTrendAnalyzer.DailyTrend> trends = trendAnalyzer.analyzeTrends(numberOfDays);

        log.debug("Retrieved {} days of trends", trends.size());
        return trends;
    }
}
