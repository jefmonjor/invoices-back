package com.invoices.invoice_service.domain.usecases;

import com.invoices.invoice_service.domain.entities.Invoice;
import com.invoices.invoice_service.domain.entities.InvoiceItem;
import com.invoices.invoice_service.domain.ports.ClientRepository;
import com.invoices.invoice_service.domain.ports.CompanyRepository;
import com.invoices.invoice_service.domain.ports.InvoiceRepository;
import com.invoices.invoice_service.exception.ClientNotFoundException;

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

    public CreateInvoiceUseCase(
        InvoiceRepository invoiceRepository,
        CompanyRepository companyRepository,
        ClientRepository clientRepository
    ) {
        this.invoiceRepository = invoiceRepository;
        this.companyRepository = companyRepository;
        this.clientRepository = clientRepository;
    }

    public Invoice execute(
        Long companyId,
        Long clientId,
        String invoiceNumber,
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

        // Set notes
        if (notes != null && !notes.trim().isEmpty()) {
            invoice.setNotes(notes);
        }

        // Save invoice
        return invoiceRepository.save(invoice);
    }
}
