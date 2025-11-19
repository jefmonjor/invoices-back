package com.invoices.invoice.domain.usecases;

import com.invoices.invoice.domain.entities.Invoice;
import com.invoices.invoice.domain.exceptions.InvoiceNotFoundException;
import com.invoices.invoice.domain.ports.InvoiceRepository;

/**
 * Use case: Retrieve invoice by ID.
 * Pure business logic with no infrastructure concerns.
 */
public class GetInvoiceByIdUseCase {
    private final InvoiceRepository repository;

    public GetInvoiceByIdUseCase(InvoiceRepository repository) {
        if (repository == null) {
            throw new IllegalArgumentException("Repository cannot be null");
        }
        this.repository = repository;
    }

    public Invoice execute(Long invoiceId) {
        validateInvoiceId(invoiceId);

        return repository.findById(invoiceId)
            .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));
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
