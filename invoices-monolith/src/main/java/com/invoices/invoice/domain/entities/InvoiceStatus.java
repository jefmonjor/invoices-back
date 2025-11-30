package com.invoices.invoice.domain.entities;

/**
 * Invoice status enum representing the lifecycle of an invoice.
 * Domain concept with business meaning.
 */
public enum InvoiceStatus {
    DRAFT("Borrador"),
    PENDING("Pendiente"),
    PAID("Pagada"),
    CANCELLED("Cancelada"),
    FINALIZED("Finalizada"),
    SENDING("Enviando"),
    SENT("Enviada"),
    REJECTED("Rechazada");

    private final String displayName;

    InvoiceStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
