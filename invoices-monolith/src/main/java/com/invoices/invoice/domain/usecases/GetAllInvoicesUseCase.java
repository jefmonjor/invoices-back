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
        // Default to first page, 50 items if not specified (soft limit to prevent OOM)
        // Ideally, we should deprecate this and force pagination
        return invoiceRepository.findSummariesByCompanyId(companyId, 0, 50);
    }

    public List<InvoiceSummary> execute(Long companyId, int page, int size) {
        return invoiceRepository.findSummariesByCompanyId(companyId, page, size);
    }
}
