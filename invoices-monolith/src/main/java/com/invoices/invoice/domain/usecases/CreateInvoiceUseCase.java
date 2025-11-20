package com.invoices.invoice.domain.usecases;

import com.invoices.invoice.domain.entities.Client;
import com.invoices.invoice.domain.entities.Invoice;
import com.invoices.invoice.domain.entities.InvoiceItem;
import com.invoices.invoice.domain.ports.ClientRepository;
import com.invoices.invoice.domain.ports.CompanyRepository;
import com.invoices.invoice.domain.ports.InvoiceEventPublisher;
import com.invoices.invoice.domain.ports.InvoiceRepository;
import com.invoices.invoice.domain.exceptions.ClientNotFoundException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Use case: Create new invoice.
 * Business logic for creating invoices with validation.
 */
public class CreateInvoiceUseCase {

    private final InvoiceRepository invoiceRepository;
    private final CompanyRepository companyRepository;
    private final ClientRepository clientRepository;
    private final InvoiceEventPublisher eventPublisher;

    public CreateInvoiceUseCase(
        InvoiceRepository invoiceRepository,
        CompanyRepository companyRepository,
        ClientRepository clientRepository,
        InvoiceEventPublisher eventPublisher
    ) {
        this.invoiceRepository = invoiceRepository;
        this.companyRepository = companyRepository;
        this.clientRepository = clientRepository;
        this.eventPublisher = eventPublisher;
    }

    public Invoice execute(
        Long companyId,
        Long clientId,
        String invoiceNumber,
        String settlementNumber,
        BigDecimal irpfPercentage,
        BigDecimal rePercentage,
        List<InvoiceItem> items,
        String notes
    ) {
        // Validate company and client exist
        if (!companyRepository.existsById(companyId)) {
            throw new IllegalArgumentException("Company not found with id: " + companyId);
        }

        if (!clientRepository.existsById(clientId)) {
            throw new ClientNotFoundException(clientId);
        }

        // Create invoice
        Invoice invoice = new Invoice(
            null, // ID will be generated
            companyId,
            clientId,
            invoiceNumber,
            LocalDateTime.now(),
            irpfPercentage != null ? irpfPercentage : BigDecimal.ZERO,
            rePercentage != null ? rePercentage : BigDecimal.ZERO
        );

        // Add items
        if (items != null) {
            items.forEach(invoice::addItem);
        }

        // Set settlement number
        if (settlementNumber != null && !settlementNumber.trim().isEmpty()) {
            invoice.setSettlementNumber(settlementNumber);
        }

        // Set notes
        if (notes != null && !notes.trim().isEmpty()) {
            invoice.setNotes(notes);
        }

        // Save invoice
        Invoice savedInvoice = invoiceRepository.save(invoice);

        // Get client email for event
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ClientNotFoundException(clientId));

        // Publish invoice created event
        eventPublisher.publishInvoiceCreated(savedInvoice, client.getEmail());

        return savedInvoice;
    }
}
