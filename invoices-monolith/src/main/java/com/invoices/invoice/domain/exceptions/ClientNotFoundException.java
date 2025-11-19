package com.invoices.invoice.domain.exceptions;

/**
 * Exception thrown when a client is not found in the system.
 * Domain-specific exception following Clean Architecture principles.
 */
public class ClientNotFoundException extends RuntimeException {
    private static final String MESSAGE_TEMPLATE = "Client with ID %d not found";

    public ClientNotFoundException(Long clientId) {
        super(String.format(MESSAGE_TEMPLATE, clientId));
    }

    public ClientNotFoundException(String message) {
        super(message);
    }

    public ClientNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
