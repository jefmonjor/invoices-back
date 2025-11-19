package com.invoices.invoice.domain.ports;

import com.invoices.invoice.domain.entities.Invoice;

/**
 * Port for publishing invoice events.
 * This is a domain interface that will be implemented by infrastructure layer.
 * Keeps domain layer independent of event infrastructure (Redis, Kafka, etc.).
 */
public interface InvoiceEventPublisher {

    /**
     * Publishes an event when an invoice is created.
     *
     * @param invoice the created invoice
     * @param clientEmail the client's email address
     */
    void publishInvoiceCreated(Invoice invoice, String clientEmail);

    /**
     * Publishes an event when an invoice is updated.
     *
     * @param invoice the updated invoice
     * @param clientEmail the client's email address
     */
    void publishInvoiceUpdated(Invoice invoice, String clientEmail);

    /**
     * Publishes an event when an invoice is paid.
     *
     * @param invoice the paid invoice
     * @param clientEmail the client's email address
     */
    void publishInvoicePaid(Invoice invoice, String clientEmail);

    /**
     * Publishes an event when an invoice is cancelled.
     *
     * @param invoice the cancelled invoice
     * @param clientEmail the client's email address
     */
    void publishInvoiceCancelled(Invoice invoice, String clientEmail);

    /**
     * Publishes an event when an invoice is deleted.
     *
     * @param invoice the deleted invoice
     * @param clientEmail the client's email address
     */
    void publishInvoiceDeleted(Invoice invoice, String clientEmail);
}
