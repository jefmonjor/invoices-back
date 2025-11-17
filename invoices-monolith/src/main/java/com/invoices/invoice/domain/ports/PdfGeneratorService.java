package com.invoices.invoice_service.domain.ports;

import com.invoices.invoice_service.domain.entities.Invoice;

/**
 * Port (interface) for PDF generation.
 * Domain layer defines what it needs, infrastructure provides the implementation.
 */
public interface PdfGeneratorService {

    /**
     * Generates a PDF document for the given invoice.
     *
     * @param invoice the invoice to generate PDF for
     * @return PDF content as byte array
     */
    byte[] generatePdf(Invoice invoice);

    /**
     * Generates a PDF document with custom configuration.
     *
     * @param invoiceNumber invoice number
     * @param baseAmount base amount
     * @param irpfPercentage IRPF percentage
     * @param rePercentage RE percentage
     * @param totalAmount total amount
     * @param color custom color (hex)
     * @param textStyle custom text style
     * @return PDF content as byte array
     */
    byte[] generateCustomPdf(
        String invoiceNumber,
        Double baseAmount,
        Double irpfPercentage,
        Double rePercentage,
        Double totalAmount,
        String color,
        String textStyle
    );
}
