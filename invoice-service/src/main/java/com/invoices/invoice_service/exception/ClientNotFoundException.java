package com.invoices.invoice_service.exception;

public class ClientNotFoundException extends RuntimeException {
    public ClientNotFoundException(String message) {
        super(message);
    }

    public ClientNotFoundException(Long id) {
        super("Cliente no encontrado con ID: " + id);
    }
}
