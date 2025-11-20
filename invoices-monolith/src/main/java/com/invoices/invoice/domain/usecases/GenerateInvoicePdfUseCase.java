package com.invoices.invoice.domain.usecases;

import com.invoices.invoice.domain.entities.Client;
import com.invoices.invoice.domain.entities.Company;
import com.invoices.invoice.domain.entities.Invoice;
import com.invoices.invoice.domain.exceptions.InvoiceNotFoundException;
import com.invoices.invoice.domain.ports.ClientRepository;
import com.invoices.invoice.domain.ports.CompanyRepository;
import com.invoices.invoice.domain.ports.InvoiceRepository;
import com.invoices.invoice.domain.ports.PdfGeneratorService;
import lombok.extern.slf4j.Slf4j;

/**
 * Use case: Generate PDF for a specific invoice.
 * Fetches invoice by ID and generates a PDF document.
 */
@Slf4j
public class GenerateInvoicePdfUseCase {

    private final InvoiceRepository invoiceRepository;
    private final CompanyRepository companyRepository;
    private final ClientRepository clientRepository;
    private final PdfGeneratorService pdfGeneratorService;

    public GenerateInvoicePdfUseCase(
            InvoiceRepository invoiceRepository,
            CompanyRepository companyRepository,
            ClientRepository clientRepository,
            PdfGeneratorService pdfGeneratorService) {
        this.invoiceRepository = invoiceRepository;
        this.companyRepository = companyRepository;
        this.clientRepository = clientRepository;
        this.pdfGeneratorService = pdfGeneratorService;
    }

    public byte[] execute(Long invoiceId) {
        log.info("Generating PDF for invoice ID: {}", invoiceId);

        // Validate input
        if (invoiceId == null || invoiceId <= 0) {
            log.error("Invalid invoice ID provided: {}", invoiceId);
            throw new IllegalArgumentException("Invoice ID must be a positive number");
        }

        try {
            // Find invoice
            Invoice invoice = invoiceRepository.findById(invoiceId)
                    .orElseThrow(() -> {
                        log.error("Invoice not found with ID: {}", invoiceId);
                        return new InvoiceNotFoundException(invoiceId);
                    });

            log.debug("Found invoice: {} (Company ID: {}, Client ID: {})",
                    invoice.getInvoiceNumber(), invoice.getCompanyId(), invoice.getClientId());

            // Validate invoice has items
            if (invoice.getItems() == null || invoice.getItems().isEmpty()) {
                log.warn("Invoice {} has no items. PDF generation may fail or produce empty invoice.",
                        invoice.getInvoiceNumber());
            }

            // Load company and client details for PDF
            Company company = companyRepository.findById(invoice.getCompanyId())
                    .orElseThrow(() -> {
                        log.error("Company not found with ID: {} for invoice: {}",
                                invoice.getCompanyId(), invoiceId);
                        return new IllegalStateException(
                                "Company not found for invoice: " + invoiceId +
                                        " (Company ID: " + invoice.getCompanyId() + ")");
                    });

            log.debug("Loaded company: {}", company.getBusinessName());

            Client client = clientRepository.findById(invoice.getClientId())
                    .orElseThrow(() -> {
                        log.error("Client not found with ID: {} for invoice: {}",
                                invoice.getClientId(), invoiceId);
                        return new IllegalStateException(
                                "Client not found for invoice: " + invoiceId +
                                        " (Client ID: " + invoice.getClientId() + ")");
                    });

            log.debug("Loaded client: {}", client.getBusinessName());

            // Set company and client on invoice for PDF generation
            invoice.setCompany(company);
            invoice.setClient(client);

            // Generate PDF
            log.info("Calling PDF generator for invoice: {}", invoice.getInvoiceNumber());
            byte[] pdfContent = pdfGeneratorService.generatePdf(invoice);

            if (pdfContent == null || pdfContent.length == 0) {
                log.error("PDF generator returned empty content for invoice: {}", invoiceId);
                throw new RuntimeException("PDF generation produced empty content");
            }

            log.info("Successfully generated PDF for invoice: {} ({} bytes)",
                    invoice.getInvoiceNumber(), pdfContent.length);

            return pdfContent;

        } catch (InvoiceNotFoundException e) {
            // Re-throw invoice not found
            throw e;
        } catch (IllegalStateException e) {
            // Re-throw company/client not found
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error generating PDF for invoice ID: {}", invoiceId, e);
            throw new RuntimeException("Error generating PDF for invoice " + invoiceId + ": " + e.getMessage(), e);
        }
    }
}
