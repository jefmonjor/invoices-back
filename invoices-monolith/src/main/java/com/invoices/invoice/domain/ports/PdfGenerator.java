package com.invoices.invoice.domain.ports;

import com.invoices.invoice.domain.entities.Client;
import com.invoices.invoice.domain.entities.Company;
import com.invoices.invoice.domain.entities.Invoice;

/**
 * Port for PDF generation service.
 * Decouples domain from specific PDF generation technology.
 */
public interface PdfGenerator {

    /**
     * Generates a PDF for the given invoice.
     *
     * @param invoice The invoice to generate PDF for
     * @param company The company issuing the invoice
     * @param client  The client receiving the invoice
     * @return The generated PDF as a byte array
     */
    byte[] generateInvoicePdf(Invoice invoice, Company company, Client client);

    /**
     * Generates a PDF for the given invoice with QR code for VeriFactu verification.
     *
     * @param invoice The invoice to generate PDF for
     * @param company The company issuing the invoice
     * @param client  The client receiving the invoice
     * @param qrPayload The QR code payload data
     * @return The generated PDF as a byte array with QR code
     */
    byte[] generateInvoicePdfWithQr(Invoice invoice, Company company, Client client, String qrPayload);
}
