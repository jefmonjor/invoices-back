package com.invoices.invoice.domain.usecases;

import com.invoices.invoice.domain.entities.Client;
import com.invoices.invoice.domain.entities.Invoice;
import com.invoices.invoice.domain.exceptions.InvalidInvoiceStateException;
import com.invoices.invoice.domain.exceptions.InvoiceNotFoundException;
import com.invoices.invoice.domain.entities.InvoiceStatus;
import com.invoices.invoice.domain.ports.ClientRepository;
import com.invoices.invoice.domain.ports.InvoiceEventPublisher;
import com.invoices.invoice.domain.ports.InvoiceRepository;
import com.invoices.invoice.domain.exceptions.ClientNotFoundException;

/**
 * Use case: Delete invoice.
 * Business logic for deleting invoices with validation.
 */
public class DeleteInvoiceUseCase {

    private final InvoiceRepository invoiceRepository;
    private final ClientRepository clientRepository;
    private final com.invoices.invoice.domain.ports.CompanyRepository companyRepository;
    private final InvoiceEventPublisher eventPublisher;

    public DeleteInvoiceUseCase(
            InvoiceRepository invoiceRepository,
            ClientRepository clientRepository,
            com.invoices.invoice.domain.ports.CompanyRepository companyRepository,
            InvoiceEventPublisher eventPublisher) {
        this.invoiceRepository = invoiceRepository;
        this.clientRepository = clientRepository;
        this.companyRepository = companyRepository;
        this.eventPublisher = eventPublisher;
    }

    public void execute(Long invoiceId) {
        // Find invoice
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));

        // Business rule: Cannot delete paid invoices
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new InvalidInvoiceStateException(
                    "Cannot delete paid invoice. Invoice ID: " + invoiceId);
        }

        // Business rule: Cannot delete ACCEPTED Veri*Factu invoices
        // They must be rectified instead
        if ("ACCEPTED".equals(invoice.getVerifactuStatus())) {
            throw new InvalidInvoiceStateException(
                    "Cannot delete an invoice accepted by Veri*Factu. Please issue a rectification invoice instead. Invoice ID: "
                            + invoiceId);
        }

        // Get client email for event before deleting
        Client client = clientRepository.findById(invoice.getClientId())
                .orElseThrow(() -> new ClientNotFoundException(invoice.getClientId()));

        // Hash Chaining Integrity:
        // If we are deleting the LAST invoice in the chain, we must "rollback" the
        // company's lastHash pointer.
        // Otherwise, the next invoice created will point to this deleted invoice's
        // hash, creating a broken link.
        try {
            com.invoices.invoice.domain.entities.Company company = companyRepository.findById(invoice.getCompanyId())
                    .orElseThrow(() -> new IllegalArgumentException("Company not found: " + invoice.getCompanyId()));

            String currentCompanyHash = company.getLastHash();
            String invoiceHash = invoice.getHash();

            // If the company's last hash matches this invoice's hash, it means this was the
            // last one.
            if (currentCompanyHash != null && currentCompanyHash.equals(invoiceHash)) {
                // Rollback to previous hash
                com.invoices.invoice.domain.entities.Company updatedCompany = company
                        .withLastHash(invoice.getPreviousDocumentHash());
                companyRepository.save(updatedCompany);
            }
        } catch (Exception e) {
            // Log but don't fail deletion if company lookup fails (consistency edge case)
            // System.out.println("Warning: Could not rollback company hash: " +
            // e.getMessage());
        }

        // Delete invoice using entity (prevents race conditions)
        invoiceRepository.delete(invoice);

        // Publish invoice deleted event
        eventPublisher.publishInvoiceDeleted(invoice, client.getEmail());
    }
}
