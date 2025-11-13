package com.invoices.invoice_service.infrastructure.config;

import com.invoices.invoice_service.domain.ports.InvoiceRepository;
import com.invoices.invoice_service.domain.ports.PdfGeneratorService;
import com.invoices.invoice_service.domain.usecases.GeneratePdfUseCase;
import com.invoices.invoice_service.domain.usecases.GetInvoiceByIdUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for use case beans.
 * Infrastructure configures domain use cases with their dependencies.
 * Dependency Injection following Clean Architecture.
 */
@Configuration
public class UseCaseConfiguration {

    @Bean
    public GetInvoiceByIdUseCase getInvoiceByIdUseCase(InvoiceRepository repository) {
        return new GetInvoiceByIdUseCase(repository);
    }

    @Bean
    public GeneratePdfUseCase generatePdfUseCase(PdfGeneratorService pdfGeneratorService) {
        return new GeneratePdfUseCase(pdfGeneratorService);
    }
}
