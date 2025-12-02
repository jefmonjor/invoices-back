package com.invoices.invoice.domain.ports;

/**
 * Port for publishing VeriFactu verification events.
 *
 * Abstracts the messaging infrastructure (Redis, Kafka, etc.).
 * Implementations are responsible for:
 * - Serializing events
 * - Publishing to message broker
 * - Handling failures
 */
public interface VerifactuVerificationPublisher {

    /**
     * Enqueue an invoice for VeriFactu verification.
     *
     * Uses default event type "INVOICE_CREATED".
     *
     * @param invoiceId ID of the invoice to verify
     * @throws VerificationEnqueueException if enqueue fails
     */
    void enqueueForVerification(Long invoiceId);

    /**
     * Enqueue an invoice with explicit event type.
     *
     * @param invoiceId ID of the invoice to verify
     * @param eventType Type of event (e.g., INVOICE_CREATED, INVOICE_UPDATED)
     * @throws VerificationEnqueueException if enqueue fails
     */
    void enqueueForVerification(Long invoiceId, String eventType);

    /**
     * Exception thrown when enqueue fails.
     */
    class VerificationEnqueueException extends RuntimeException {
        public VerificationEnqueueException(String message) {
            super(message);
        }

        public VerificationEnqueueException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
