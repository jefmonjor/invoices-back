package com.invoices.invoice_service.exception;

public class InvoiceNotFoundException extends RuntimeException {
    public InvoiceNotFoundException(String message) {
        super(message);
    }

    public InvoiceNotFoundException(Long id) {
        super("Factura no encontrada con ID: " + id);
    }
}
