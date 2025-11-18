package com.invoices.invoice.domain.usecases;

import com.invoices.invoice.domain.entities.Client;
import com.invoices.invoice.domain.entities.Invoice;
import com.invoices.invoice.domain.exceptions.InvalidInvoiceStateException;
import com.invoices.invoice.domain.exceptions.InvoiceNotFoundException;
import com.invoices.invoice.domain.entities.InvoiceStatus;
import com.invoices.invoice.domain.ports.ClientRepository;
import com.invoices.invoice.domain.ports.InvoiceEventPublisher;
import com.invoices.invoice.domain.ports.InvoiceRepository;
import com.invoices.invoice.exception.ClientNotFoundException;

/**
 * Use case: Delete invoice.
 * Business logic for deleting invoices with validation.
 */
public class DeleteInvoiceUseCase {

    private final InvoiceRepository invoiceRepository;
    private final ClientRepository clientRepository;
    private final InvoiceEventPublisher eventPublisher;

    public DeleteInvoiceUseCase(
            InvoiceRepository invoiceRepository,
            ClientRepository clientRepository,
            InvoiceEventPublisher eventPublisher
    ) {
        this.invoiceRepository = invoiceRepository;
        this.clientRepository = clientRepository;
        this.eventPublisher = eventPublisher;
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

        // Get client email for event before deleting
        Client client = clientRepository.findById(invoice.getClientId())
                .orElseThrow(() -> new ClientNotFoundException(invoice.getClientId()));

        // Delete invoice using entity (prevents race conditions)
        // This ensures we delete the exact entity we validated, not just by ID
        invoiceRepository.delete(invoice);

        // Publish invoice deleted event
        eventPublisher.publishInvoiceDeleted(invoice, client.getEmail());
    }
}
