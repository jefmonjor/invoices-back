package com.invoices.invoice.domain.exceptions;

/**
 * Exception thrown when attempting an invalid operation on an invoice.
 * Examples: modifying finalized invoice, finalizing empty invoice.
 */
public class InvalidInvoiceStateException extends RuntimeException {

    public InvalidInvoiceStateException(String message) {
        super(message);
    }

    public InvalidInvoiceStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
