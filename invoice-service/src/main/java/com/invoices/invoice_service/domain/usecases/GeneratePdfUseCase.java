package com.invoices.invoice_service.domain.usecases;

import com.invoices.invoice_service.domain.ports.PdfGeneratorService;

/**
 * Use case: Generate PDF with custom configuration.
 * Business logic for PDF generation without knowing implementation details.
 */
public class GeneratePdfUseCase {
    private final PdfGeneratorService pdfGeneratorService;

    public GeneratePdfUseCase(PdfGeneratorService pdfGeneratorService) {
        if (pdfGeneratorService == null) {
            throw new IllegalArgumentException("PDF Generator Service cannot be null");
        }
        this.pdfGeneratorService = pdfGeneratorService;
    }

    public byte[] execute(
        String invoiceNumber,
        Double baseAmount,
        Double irpfPercentage,
        Double rePercentage,
        Double totalAmount,
        String color,
        String textStyle
    ) {
        validateInputs(invoiceNumber, baseAmount, totalAmount);

        return pdfGeneratorService.generateCustomPdf(
            invoiceNumber,
            baseAmount,
            irpfPercentage != null ? irpfPercentage : 0.0,
            rePercentage != null ? rePercentage : 0.0,
            totalAmount,
            color != null ? color : "#FFFFFF",
            textStyle != null ? textStyle : "normal"
        );
    }

    private void validateInputs(String invoiceNumber, Double baseAmount, Double totalAmount) {
        if (invoiceNumber == null || invoiceNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Invoice number cannot be null or empty");
        }
        if (baseAmount == null || baseAmount <= 0) {
            throw new IllegalArgumentException("Base amount must be positive");
        }
        if (totalAmount == null || totalAmount <= 0) {
            throw new IllegalArgumentException("Total amount must be positive");
        }
    }
}
