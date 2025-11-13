package com.invoices.invoice_service.domain.usecases;

import com.invoices.invoice_service.domain.entities.Invoice;
import com.invoices.invoice_service.domain.entities.InvoiceItem;
import com.invoices.invoice_service.domain.exceptions.InvoiceNotFoundException;
import com.invoices.invoice_service.domain.ports.InvoiceRepository;

import java.util.List;

/**
 * Use case: Update existing invoice.
 * Business logic for updating invoice details.
 */
public class UpdateInvoiceUseCase {

    private final InvoiceRepository invoiceRepository;

    public UpdateInvoiceUseCase(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    public Invoice execute(
        Long invoiceId,
        List<InvoiceItem> updatedItems,
        String notes
    ) {
        // Find existing invoice
        Invoice invoice = invoiceRepository.findById(invoiceId)
            .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));

        // Update items if provided
        if (updatedItems != null) {
            // Clear existing items
            invoice.getItems().clear();

            // Add updated items
            updatedItems.forEach(invoice::addItem);
        }

        // Update notes if provided
        if (notes != null) {
            invoice.setNotes(notes);
        }

        // Save updated invoice
        return invoiceRepository.save(invoice);
    }
}
