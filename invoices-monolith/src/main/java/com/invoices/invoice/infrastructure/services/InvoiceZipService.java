package com.invoices.invoice.infrastructure.services;

import com.invoices.invoice.domain.entities.Client;
import com.invoices.invoice.domain.entities.Company;
import com.invoices.invoice.domain.entities.Invoice;
import com.invoices.invoice.domain.ports.ClientRepository;
import com.invoices.invoice.domain.ports.PdfGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Service for generating ZIP archives containing invoice PDFs.
 * Used for quarterly and yearly batch downloads.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceZipService {

    private final PdfGenerator pdfGenerator;
    private final ClientRepository clientRepository;

    /**
     * Generates a ZIP archive containing PDFs for all provided invoices.
     *
     * @param invoices List of invoices to include in the ZIP
     * @param company  The company that owns the invoices
     * @return byte array containing the ZIP file
     * @throws IOException if PDF generation or ZIP creation fails
     */
    public byte[] generateInvoicesZip(List<Invoice> invoices, Company company) throws IOException {
        log.info("Generating ZIP for {} invoices of company {}", invoices.size(), company.getId());

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ZipOutputStream zos = new ZipOutputStream(baos)) {

            int successCount = 0;
            int errorCount = 0;

            for (Invoice invoice : invoices) {
                try {
                    // Get client for this invoice
                    Client client = clientRepository.findById(invoice.getClientId())
                            .orElseThrow(() -> new RuntimeException("Client not found: " + invoice.getClientId()));

                    // Generate PDF bytes
                    byte[] pdfBytes;
                    if ("ACCEPTED".equals(invoice.getVerifactuStatus()) &&
                            invoice.getQrPayload() != null && !invoice.getQrPayload().isEmpty()) {
                        // Generate final PDF with QR for accepted invoices
                        pdfBytes = pdfGenerator.generateInvoicePdfWithQr(invoice, company, client,
                                invoice.getQrPayload());
                    } else {
                        // Generate draft PDF for other invoices
                        pdfBytes = pdfGenerator.generateInvoicePdf(invoice, company, client);
                    }

                    // Create filename - sanitize invoice number for filesystem
                    String filename = String.format("Factura_%s.pdf",
                            invoice.getInvoiceNumber().replace("/", "_").replace("\\", "_"));

                    // Add to ZIP
                    ZipEntry zipEntry = new ZipEntry(filename);
                    zos.putNextEntry(zipEntry);
                    zos.write(pdfBytes);
                    zos.closeEntry();

                    successCount++;
                    log.debug("Added invoice {} to ZIP", invoice.getInvoiceNumber());

                } catch (Exception e) {
                    errorCount++;
                    log.error("Error generating PDF for invoice {}: {}", invoice.getInvoiceNumber(), e.getMessage());
                    // Continue with other invoices instead of failing completely
                }
            }

            zos.finish();
            log.info("ZIP generation complete. Success: {}, Errors: {}", successCount, errorCount);

            if (successCount == 0 && errorCount > 0) {
                throw new IOException("Failed to generate any PDFs for the ZIP archive");
            }

            return baos.toByteArray();
        }
    }

    /**
     * Generates a ZIP archive for a specific quarter.
     */
    public byte[] generateQuarterZip(List<Invoice> invoices, Company company, int year, int quarter)
            throws IOException {
        log.info("Generating Q{} {} ZIP for company {}", quarter, year, company.getId());
        return generateInvoicesZip(invoices, company);
    }

    /**
     * Generates a ZIP archive for a full year.
     */
    public byte[] generateYearZip(List<Invoice> invoices, Company company, int year) throws IOException {
        log.info("Generating {} year ZIP for company {}", year, company.getId());
        return generateInvoicesZip(invoices, company);
    }
}
