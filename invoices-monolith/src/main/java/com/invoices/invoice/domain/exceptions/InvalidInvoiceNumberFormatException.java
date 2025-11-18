package com.invoices.invoice.domain.exceptions;

/**
 * Exception thrown when invoice number format is invalid.
 * Expected format: YYYY-XXX (e.g., 2025-001).
 */
public class InvalidInvoiceNumberFormatException extends RuntimeException {
    private static final String MESSAGE_TEMPLATE =
        "Invalid invoice number format: '%s'. Expected format: YYYY-XXX";

    public InvalidInvoiceNumberFormatException(String invoiceNumber) {
        super(String.format(MESSAGE_TEMPLATE, invoiceNumber));
    }
}
