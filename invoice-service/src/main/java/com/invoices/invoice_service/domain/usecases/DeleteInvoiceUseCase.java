package com.invoices.invoice_service.domain.usecases;

import com.invoices.invoice_service.domain.entities.Invoice;
import com.invoices.invoice_service.domain.exceptions.InvalidInvoiceStateException;
import com.invoices.invoice_service.domain.exceptions.InvoiceNotFoundException;
import com.invoices.invoice_service.domain.entities.InvoiceStatus;
import com.invoices.invoice_service.domain.ports.InvoiceRepository;

/**
 * Use case: Delete invoice.
 * Business logic for deleting invoices with validation.
 */
public class DeleteInvoiceUseCase {

    private final InvoiceRepository invoiceRepository;

    public DeleteInvoiceUseCase(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    public void execute(Long invoiceId) {
        // Find invoice
        Invoice invoice = invoiceRepository.findById(invoiceId)
            .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));

        // Business rule: Cannot delete paid invoices
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new InvalidInvoiceStateException(
                "Cannot delete paid invoice. Invoice ID: " + invoiceId
            );
        }

        // Delete invoice
        invoiceRepository.deleteById(invoiceId);
    }
}
