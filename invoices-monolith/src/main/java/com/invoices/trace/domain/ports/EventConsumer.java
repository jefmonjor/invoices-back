package com.invoices.trace.domain.ports;

import com.invoices.trace.domain.events.InvoiceEvent;

/**
 * Port (interface) for consuming events.
 * This abstracts the underlying messaging implementation (Redis Streams, Kafka, etc.).
 * Infrastructure layer will provide the actual implementation.
 */
public interface EventConsumer {

    /**
     * Processes an invoice event.
     *
     * @param event the invoice event to process
     * @throws RuntimeException if processing fails after all retries
     */
    void processEvent(InvoiceEvent event);

    /**
     * Sends a failed event to the Dead Letter Queue.
     *
     * @param event     the event that failed to process
     * @param exception the exception that caused the failure
     */
    void sendToDLQ(InvoiceEvent event, Exception exception);
}
