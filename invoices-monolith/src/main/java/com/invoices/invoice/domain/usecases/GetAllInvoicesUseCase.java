package com.invoices.invoice_service.domain.usecases;

import com.invoices.invoice_service.domain.entities.Invoice;
import com.invoices.invoice_service.domain.ports.InvoiceRepository;

import java.util.List;

/**
 * Use case: Get all invoices.
 * Business logic for retrieving all invoices.
 */
public class GetAllInvoicesUseCase {

    private final InvoiceRepository invoiceRepository;

    public GetAllInvoicesUseCase(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    public List<Invoice> execute() {
        return invoiceRepository.findAll();
    }
}
