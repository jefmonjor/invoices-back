package com.invoices.invoice.infrastructure.events;

import com.invoices.invoice.domain.entities.Invoice;
import com.invoices.invoice.domain.ports.InvoiceEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * No-op implementation of InvoiceEventPublisher.
 * Events are logged but not sent to any external system.
 * 
 * For future scaling, replace with Redis Streams or Kafka implementation.
 * See: FEATURES_PARA_ESCALAR.md
 */
@Component
@Slf4j
public class NoOpInvoiceEventPublisher implements InvoiceEventPublisher {

    @Override
    public void publishInvoiceCreated(Invoice invoice, String clientEmail) {
        log.debug("Invoice created event (no-op): {} to {}", invoice.getId(), clientEmail);
    }

    @Override
    public void publishInvoiceUpdated(Invoice invoice, String clientEmail) {
        log.debug("Invoice updated event (no-op): {} to {}", invoice.getId(), clientEmail);
    }

    @Override
    public void publishInvoicePaid(Invoice invoice, String clientEmail) {
        log.debug("Invoice paid event (no-op): {} to {}", invoice.getId(), clientEmail);
    }

    @Override
    public void publishInvoiceCancelled(Invoice invoice, String clientEmail) {
        log.debug("Invoice cancelled event (no-op): {} to {}", invoice.getId(), clientEmail);
    }

    @Override
    public void publishInvoiceDeleted(Invoice invoice, String clientEmail) {
        log.debug("Invoice deleted event (no-op): {} to {}", invoice.getId(), clientEmail);
    }
}
