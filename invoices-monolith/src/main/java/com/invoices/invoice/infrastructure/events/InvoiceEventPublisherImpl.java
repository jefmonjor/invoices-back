package com.invoices.invoice.infrastructure.events;

import com.invoices.invoice.domain.entities.Invoice;
import com.invoices.invoice.domain.ports.InvoiceEventPublisher;
import com.invoices.trace.domain.events.InvoiceEvent;
import com.invoices.invoice.events.InvoiceEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Implementation of InvoiceEventPublisher using Redis Streams.
 * Adapts the domain port to the infrastructure InvoiceEventProducer.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InvoiceEventPublisherImpl implements InvoiceEventPublisher {

    private final InvoiceEventProducer eventProducer;

    @Override
    public void publishInvoiceCreated(Invoice invoice, String clientEmail) {
        try {
            InvoiceEvent event = createEvent("INVOICE_CREATED", invoice, clientEmail);
            eventProducer.sendInvoiceCreated(event);
            log.info("Published INVOICE_CREATED event for invoice: {}", invoice.getInvoiceNumber());
        } catch (Exception e) {
            log.error("Failed to publish INVOICE_CREATED event for invoice: {}",
                    invoice.getInvoiceNumber(), e);
            // Don't throw exception to avoid breaking the business operation
        }
    }

    @Override
    public void publishInvoiceUpdated(Invoice invoice, String clientEmail) {
        try {
            InvoiceEvent event = createEvent("INVOICE_UPDATED", invoice, clientEmail);
            eventProducer.sendInvoiceUpdated(event);
            log.info("Published INVOICE_UPDATED event for invoice: {}", invoice.getInvoiceNumber());
        } catch (Exception e) {
            log.error("Failed to publish INVOICE_UPDATED event for invoice: {}",
                    invoice.getInvoiceNumber(), e);
        }
    }

    @Override
    public void publishInvoicePaid(Invoice invoice, String clientEmail) {
        try {
            InvoiceEvent event = createEvent("INVOICE_PAID", invoice, clientEmail);
            eventProducer.sendInvoicePaid(event);
            log.info("Published INVOICE_PAID event for invoice: {}", invoice.getInvoiceNumber());
        } catch (Exception e) {
            log.error("Failed to publish INVOICE_PAID event for invoice: {}",
                    invoice.getInvoiceNumber(), e);
        }
    }

    @Override
    public void publishInvoiceCancelled(Invoice invoice, String clientEmail) {
        try {
            InvoiceEvent event = createEvent("INVOICE_CANCELLED", invoice, clientEmail);
            eventProducer.sendInvoiceCancelled(event);
            log.info("Published INVOICE_CANCELLED event for invoice: {}", invoice.getInvoiceNumber());
        } catch (Exception e) {
            log.error("Failed to publish INVOICE_CANCELLED event for invoice: {}",
                    invoice.getInvoiceNumber(), e);
        }
    }

    @Override
    public void publishInvoiceDeleted(Invoice invoice, String clientEmail) {
        try {
            // Use INVOICE_CANCELLED event type for deletions
            InvoiceEvent event = createEvent("INVOICE_DELETED", invoice, clientEmail);
            eventProducer.sendInvoiceCancelled(event);
            log.info("Published INVOICE_DELETED event for invoice: {}", invoice.getInvoiceNumber());
        } catch (Exception e) {
            log.error("Failed to publish INVOICE_DELETED event for invoice: {}",
                    invoice.getInvoiceNumber(), e);
        }
    }

    private InvoiceEvent createEvent(String eventType, Invoice invoice, String clientEmail) {
        return new InvoiceEvent(
                eventType,
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getClientId(),
                clientEmail,
                invoice.calculateTotalAmount(),
                invoice.getStatus().name(),
                LocalDateTime.now()
        );
    }
}
