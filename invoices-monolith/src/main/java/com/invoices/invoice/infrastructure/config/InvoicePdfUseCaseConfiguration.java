package com.invoices.invoice.infrastructure.config;

import com.invoices.invoice.domain.ports.InvoiceRepository;
import com.invoices.invoice.domain.ports.VerifactuVerificationPublisher;
import com.invoices.invoice.domain.usecases.GeneratePdfWithVerificationUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for Invoice PDF use cases.
 *
 * Provides beans for PDF generation and verification use cases.
 * Separates use case instantiation from controllers.
 * Makes use cases testable and injectable.
 */
@Configuration
public class InvoicePdfUseCaseConfiguration {

    @Bean
    public GeneratePdfWithVerificationUseCase generatePdfWithVerificationUseCase(
            InvoiceRepository invoiceRepository,
            VerifactuVerificationPublisher verificationPublisher) {
        return new GeneratePdfWithVerificationUseCase(invoiceRepository, verificationPublisher);
    }
}
