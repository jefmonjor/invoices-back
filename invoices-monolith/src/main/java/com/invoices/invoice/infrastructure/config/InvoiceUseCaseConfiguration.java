package com.invoices.invoice.infrastructure.config;

import com.invoices.invoice.domain.ports.ClientRepository;
import com.invoices.invoice.domain.ports.CompanyRepository;
import com.invoices.invoice.domain.ports.InvoiceEventPublisher;
import com.invoices.invoice.domain.ports.InvoiceRepository;
import com.invoices.invoice.domain.usecases.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for invoice use case beans.
 * Infrastructure configures domain use cases with their dependencies.
 * Dependency Injection following Clean Architecture.
 */
@Configuration
public class InvoiceUseCaseConfiguration {

    @Bean
    public GetInvoiceByIdUseCase getInvoiceByIdUseCase(
            InvoiceRepository invoiceRepository,
            ClientRepository clientRepository,
            CompanyRepository companyRepository) {
        return new GetInvoiceByIdUseCase(invoiceRepository, clientRepository, companyRepository);
    }

    @Bean
    public GetAllInvoicesUseCase getAllInvoicesUseCase(InvoiceRepository repository) {
        return new GetAllInvoicesUseCase(repository);
    }

    @Bean
    public CreateInvoiceUseCase createInvoiceUseCase(
            InvoiceRepository invoiceRepository,
            CompanyRepository companyRepository,
            ClientRepository clientRepository,
            InvoiceEventPublisher eventPublisher,
            com.invoices.invoice.domain.services.InvoiceNumberingService invoiceNumberingService,
            com.invoices.verifactu.application.services.InvoiceChainService invoiceChainService) {
        return new CreateInvoiceUseCase(
                invoiceRepository,
                companyRepository,
                clientRepository,
                eventPublisher,
                invoiceNumberingService,
                invoiceChainService);
    }

    @Bean
    public UpdateInvoiceUseCase updateInvoiceUseCase(
            InvoiceRepository repository,
            ClientRepository clientRepository,
            InvoiceEventPublisher eventPublisher,
            CompanyRepository companyRepository,
            com.invoices.verifactu.application.services.InvoiceChainService invoiceChainService) {
        return new UpdateInvoiceUseCase(repository, clientRepository, eventPublisher, companyRepository,
                invoiceChainService);
    }

    @Bean
    public DeleteInvoiceUseCase deleteInvoiceUseCase(
            InvoiceRepository repository,
            ClientRepository clientRepository,
            InvoiceEventPublisher eventPublisher) {
        return new DeleteInvoiceUseCase(repository, clientRepository, eventPublisher);
    }
}
