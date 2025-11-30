package com.invoices.invoice.domain.usecases;

import com.invoices.invoice.domain.models.InvoiceSummary;
import com.invoices.invoice.domain.ports.InvoiceRepository;

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

    public List<InvoiceSummary> execute(Long companyId) {
        return invoiceRepository.findSummariesByCompanyId(companyId);
    }
}
