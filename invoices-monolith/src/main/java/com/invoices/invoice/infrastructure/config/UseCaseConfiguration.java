package com.invoices.invoice.infrastructure.config;

import com.invoices.invoice.domain.ports.ClientRepository;
import com.invoices.invoice.domain.ports.CompanyRepository;
import com.invoices.invoice.domain.ports.InvoiceRepository;
import com.invoices.invoice.domain.ports.PdfGeneratorService;
import com.invoices.invoice.domain.usecases.*;
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
    public GetAllInvoicesUseCase getAllInvoicesUseCase(InvoiceRepository repository) {
        return new GetAllInvoicesUseCase(repository);
    }

    @Bean
    public CreateInvoiceUseCase createInvoiceUseCase(
        InvoiceRepository invoiceRepository,
        CompanyRepository companyRepository,
        ClientRepository clientRepository
    ) {
        return new CreateInvoiceUseCase(invoiceRepository, companyRepository, clientRepository);
    }

    @Bean
    public UpdateInvoiceUseCase updateInvoiceUseCase(InvoiceRepository repository) {
        return new UpdateInvoiceUseCase(repository);
    }

    @Bean
    public DeleteInvoiceUseCase deleteInvoiceUseCase(InvoiceRepository repository) {
        return new DeleteInvoiceUseCase(repository);
    }

    @Bean
    public GeneratePdfUseCase generatePdfUseCase(PdfGeneratorService pdfGeneratorService) {
        return new GeneratePdfUseCase(pdfGeneratorService);
    }
}
