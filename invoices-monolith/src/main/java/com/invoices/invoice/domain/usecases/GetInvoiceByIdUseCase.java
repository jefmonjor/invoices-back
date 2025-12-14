package com.invoices.invoice.domain.usecases;

import com.invoices.invoice.domain.entities.Client;
import com.invoices.invoice.domain.entities.Company;
import com.invoices.invoice.domain.entities.Invoice;
import com.invoices.invoice.domain.exceptions.InvoiceNotFoundException;
import com.invoices.invoice.domain.ports.ClientRepository;
import com.invoices.invoice.domain.ports.CompanyRepository;
import com.invoices.invoice.domain.ports.InvoiceRepository;

/**
 * Use case: Retrieve invoice by ID.
 * Populates company and client data for complete invoice response.
 */
public class GetInvoiceByIdUseCase {
    private final InvoiceRepository invoiceRepository;
    private final ClientRepository clientRepository;
    private final CompanyRepository companyRepository;

    public GetInvoiceByIdUseCase(
            InvoiceRepository invoiceRepository,
            ClientRepository clientRepository,
            CompanyRepository companyRepository) {
        if (invoiceRepository == null) {
            throw new IllegalArgumentException("Invoice repository cannot be null");
        }
        if (clientRepository == null) {
            throw new IllegalArgumentException("Client repository cannot be null");
        }
        if (companyRepository == null) {
            throw new IllegalArgumentException("Company repository cannot be null");
        }
        this.invoiceRepository = invoiceRepository;
        this.clientRepository = clientRepository;
        this.companyRepository = companyRepository;
    }

    public Invoice execute(Long invoiceId) {
        validateInvoiceId(invoiceId);

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));

        // Populate client data for complete DTO response
        if (invoice.getClientId() != null && invoice.getClient() == null) {
            Client client = clientRepository.findById(invoice.getClientId()).orElse(null);
            invoice.setClient(client);
        }

        // Populate company data for complete DTO response
        if (invoice.getCompanyId() != null && invoice.getCompany() == null) {
            Company company = companyRepository.findById(invoice.getCompanyId()).orElse(null);
            invoice.setCompany(company);
        }

        return invoice;
    }

    private void validateInvoiceId(Long invoiceId) {
        if (invoiceId == null) {
            throw new IllegalArgumentException("Invoice ID cannot be null");
        }
        if (invoiceId <= 0) {
            throw new IllegalArgumentException("Invoice ID must be positive");
        }
    }
}
