package com.invoices.invoice.domain.usecases;

import com.invoices.invoice.domain.entities.Client;
import com.invoices.invoice.domain.entities.Invoice;
import com.invoices.invoice.domain.entities.InvoiceItem;
import com.invoices.invoice.domain.exceptions.InvoiceNotFoundException;
import com.invoices.invoice.domain.ports.ClientRepository;
import com.invoices.invoice.domain.ports.InvoiceEventPublisher;
import com.invoices.invoice.domain.ports.InvoiceRepository;
import com.invoices.invoice.domain.exceptions.ClientNotFoundException;

import java.math.BigDecimal;
import java.util.List;

/**
 * Use case: Update existing invoice.
 * Business logic for updating invoice details.
 *
 * Note: Core invoice fields (companyId, clientId, invoiceNumber, irpfPercentage, rePercentage)
 * are immutable and cannot be changed after creation. Only notes, settlementNumber, and items can be updated.
 */
public class UpdateInvoiceUseCase {

    private final InvoiceRepository invoiceRepository;
    private final ClientRepository clientRepository;
    private final InvoiceEventPublisher eventPublisher;

    public UpdateInvoiceUseCase(
            InvoiceRepository invoiceRepository,
            ClientRepository clientRepository,
            InvoiceEventPublisher eventPublisher
    ) {
        this.invoiceRepository = invoiceRepository;
        this.clientRepository = clientRepository;
        this.eventPublisher = eventPublisher;
    }

    public Invoice execute(
        Long invoiceId,
        Long companyId,
        Long clientId,
        String invoiceNumber,
        String settlementNumber,
        BigDecimal irpfPercentage,
        BigDecimal rePercentage,
        List<InvoiceItem> updatedItems,
        String notes
    ) {
        // Find existing invoice
        Invoice invoice = invoiceRepository.findById(invoiceId)
            .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));

        // Validate immutable fields haven't changed (if provided)
        if (companyId != null && !companyId.equals(invoice.getCompanyId())) {
            throw new IllegalArgumentException(
                "Cannot change company ID. Current: " + invoice.getCompanyId() + ", Requested: " + companyId
            );
        }

        if (clientId != null && !clientId.equals(invoice.getClientId())) {
            throw new IllegalArgumentException(
                "Cannot change client ID. Current: " + invoice.getClientId() + ", Requested: " + clientId
            );
        }

        if (invoiceNumber != null && !invoiceNumber.equals(invoice.getInvoiceNumber())) {
            throw new IllegalArgumentException(
                "Cannot change invoice number. Current: " + invoice.getInvoiceNumber() + ", Requested: " + invoiceNumber
            );
        }

        if (irpfPercentage != null && invoice.getIrpfPercentage().compareTo(irpfPercentage) != 0) {
            throw new IllegalArgumentException(
                "Cannot change IRPF percentage. Current: " + invoice.getIrpfPercentage() + "%, Requested: " + irpfPercentage + "%"
            );
        }

        if (rePercentage != null && invoice.getRePercentage().compareTo(rePercentage) != 0) {
            throw new IllegalArgumentException(
                "Cannot change RE percentage. Current: " + invoice.getRePercentage() + "%, Requested: " + rePercentage + "%"
            );
        }

        // Update settlement number if provided
        if (settlementNumber != null) {
            invoice.setSettlementNumber(settlementNumber);
        }

        // Update items if provided
        if (updatedItems != null) {
            // Clear existing items using the proper domain method
            invoice.clearItems();

            // Add updated items
            updatedItems.forEach(invoice::addItem);
        }

        // Update notes if provided
        if (notes != null) {
            invoice.setNotes(notes);
        }

        // Save updated invoice
        Invoice updatedInvoice = invoiceRepository.save(invoice);

        // Get client email for event
        Client client = clientRepository.findById(invoice.getClientId())
                .orElseThrow(() -> new ClientNotFoundException(invoice.getClientId()));

        // Publish invoice updated event
        eventPublisher.publishInvoiceUpdated(updatedInvoice, client.getEmail());

        return updatedInvoice;
    }
}
