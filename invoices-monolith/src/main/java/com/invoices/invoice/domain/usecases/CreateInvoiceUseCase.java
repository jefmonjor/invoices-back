package com.invoices.invoice.domain.usecases;

import com.invoices.invoice.domain.entities.Client;
import com.invoices.invoice.domain.entities.Company;
import com.invoices.invoice.domain.entities.Invoice;
import com.invoices.invoice.domain.entities.InvoiceItem;
import com.invoices.invoice.domain.ports.ClientRepository;
import com.invoices.invoice.domain.ports.CompanyRepository;
import com.invoices.invoice.domain.ports.InvoiceEventPublisher;
import com.invoices.invoice.domain.ports.InvoiceRepository;
import com.invoices.invoice.domain.exceptions.ClientNotFoundException;
import com.invoices.verifactu.application.services.InvoiceChainService;

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
    private final com.invoices.invoice.domain.services.InvoiceNumberingService invoiceNumberingService;
    private final InvoiceChainService invoiceChainService;

    public CreateInvoiceUseCase(
            InvoiceRepository invoiceRepository,
            CompanyRepository companyRepository,
            ClientRepository clientRepository,
            InvoiceEventPublisher eventPublisher,
            com.invoices.invoice.domain.services.InvoiceNumberingService invoiceNumberingService,
            InvoiceChainService invoiceChainService) {
        this.invoiceRepository = invoiceRepository;
        this.companyRepository = companyRepository;
        this.clientRepository = clientRepository;
        this.eventPublisher = eventPublisher;
        this.invoiceNumberingService = invoiceNumberingService;
        this.invoiceChainService = invoiceChainService;
    }

    public Invoice execute(
            Long companyId,
            Long clientId,
            // invoiceNumber is now generated internally
            String settlementNumber,
            BigDecimal irpfPercentage,
            BigDecimal rePercentage,
            List<InvoiceItem> items,
            String notes) {
        // Validate company exists and get it for hash chaining
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found with id: " + companyId));

        if (!clientRepository.existsById(clientId)) {
            throw new ClientNotFoundException(clientId);
        }

        // Generate invoice number
        String invoiceNumber = invoiceNumberingService.generateNextNumber(companyId);

        // Create invoice
        Invoice invoice = new Invoice(
                null, // ID will be generated
                companyId,
                clientId,
                invoiceNumber,
                LocalDateTime.now(),
                irpfPercentage != null ? irpfPercentage : BigDecimal.ZERO,
                rePercentage != null ? rePercentage : BigDecimal.ZERO);

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

        // Calculate VeriFactu hash (chaining with previous invoice)
        invoiceChainService.prepareInvoiceForChaining(invoice, company);

        // Save invoice
        Invoice savedInvoice = invoiceRepository.save(invoice);

        // Update company's lastHash for next invoice in chain
        if (savedInvoice.getHash() != null) {
            Company updatedCompany = company.withLastHash(savedInvoice.getHash());
            companyRepository.save(updatedCompany);
        }

        // Get client email for event
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ClientNotFoundException(clientId));

        // Publish invoice created event
        eventPublisher.publishInvoiceCreated(savedInvoice, client.getEmail());

        return savedInvoice;
    }
}
