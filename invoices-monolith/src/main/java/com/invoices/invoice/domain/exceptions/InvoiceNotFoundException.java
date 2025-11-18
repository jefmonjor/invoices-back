package com.invoices.invoice.domain.exceptions;

/**
 * Exception thrown when an invoice is not found in the system.
 * Domain-specific exception following Clean Architecture principles.
 */
public class InvoiceNotFoundException extends RuntimeException {
    private static final String MESSAGE_TEMPLATE = "Invoice with ID %d not found";

    public InvoiceNotFoundException(Long invoiceId) {
        super(String.format(MESSAGE_TEMPLATE, invoiceId));
    }

    public InvoiceNotFoundException(String message) {
        super(message);
    }

    public InvoiceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
