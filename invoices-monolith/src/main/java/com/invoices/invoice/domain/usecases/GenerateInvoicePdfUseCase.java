package com.invoices.invoice.domain.usecases;

import com.invoices.invoice.domain.entities.Client;
import com.invoices.invoice.domain.entities.Company;
import com.invoices.invoice.domain.entities.Invoice;
import com.invoices.invoice.domain.exceptions.InvoiceNotFoundException;
import com.invoices.invoice.domain.ports.ClientRepository;
import com.invoices.invoice.domain.ports.CompanyRepository;
import com.invoices.invoice.domain.ports.InvoiceRepository;
import com.invoices.invoice.domain.ports.PdfGeneratorService;

/**
 * Use case: Generate PDF for a specific invoice.
 * Fetches invoice by ID and generates a PDF document.
 */
public class GenerateInvoicePdfUseCase {

    private final InvoiceRepository invoiceRepository;
    private final CompanyRepository companyRepository;
    private final ClientRepository clientRepository;
    private final PdfGeneratorService pdfGeneratorService;

    public GenerateInvoicePdfUseCase(
        InvoiceRepository invoiceRepository,
        CompanyRepository companyRepository,
        ClientRepository clientRepository,
        PdfGeneratorService pdfGeneratorService
    ) {
        this.invoiceRepository = invoiceRepository;
        this.companyRepository = companyRepository;
        this.clientRepository = clientRepository;
        this.pdfGeneratorService = pdfGeneratorService;
    }

    public byte[] execute(Long invoiceId) {
        // Find invoice
        Invoice invoice = invoiceRepository.findById(invoiceId)
            .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));

        // Load company and client details for PDF
        Company company = companyRepository.findById(invoice.getCompanyId())
            .orElseThrow(() -> new IllegalStateException("Company not found for invoice: " + invoiceId));

        Client client = clientRepository.findById(invoice.getClientId())
            .orElseThrow(() -> new IllegalStateException("Client not found for invoice: " + invoiceId));

        // Set company and client on invoice for PDF generation
        invoice.setCompany(company);
        invoice.setClient(client);

        // Generate PDF
        return pdfGeneratorService.generatePdf(invoice);
    }
}
