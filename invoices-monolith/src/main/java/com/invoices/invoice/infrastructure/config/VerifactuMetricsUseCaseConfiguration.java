package com.invoices.invoice.infrastructure.config;

import com.invoices.invoice.domain.ports.DLQMonitorService;
import com.invoices.invoice.domain.ports.VerifactuMetricsRepository;
import com.invoices.invoice.domain.usecases.GetVerifactuMetricsUseCase;
import com.invoices.invoice.domain.usecases.GetVerifactuTrendsUseCase;
import com.invoices.verifactu.domain.services.VerifactuErrorAnalyzer;
import com.invoices.verifactu.domain.services.VerifactuTrendAnalyzer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for VeriFactu metrics use cases.
 *
 * Provides beans for all VeriFactu metrics-related use cases.
 * Separates use case instantiation from controllers.
 * Makes use cases testable and injectable.
 */
@Configuration
public class VerifactuMetricsUseCaseConfiguration {

    @Bean
    public VerifactuErrorAnalyzer verifactuErrorAnalyzer(
            VerifactuMetricsRepository metricsRepository) {
        return new VerifactuErrorAnalyzer(metricsRepository);
    }

    @Bean
    public VerifactuTrendAnalyzer verifactuTrendAnalyzer(
            VerifactuMetricsRepository metricsRepository) {
        return new VerifactuTrendAnalyzer(metricsRepository);
    }

    @Bean
    public GetVerifactuMetricsUseCase getVerifactuMetricsUseCase(
            VerifactuMetricsRepository metricsRepository,
            DLQMonitorService dlqMonitor,
            VerifactuErrorAnalyzer errorAnalyzer) {
        return new GetVerifactuMetricsUseCase(metricsRepository, dlqMonitor, errorAnalyzer);
    }

    @Bean
    public GetVerifactuTrendsUseCase getVerifactuTrendsUseCase(
            VerifactuTrendAnalyzer trendAnalyzer) {
        return new GetVerifactuTrendsUseCase(trendAnalyzer);
    }
}
