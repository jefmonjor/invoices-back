package com.invoices.trace_service.kafka;

import com.invoices.trace_service.dto.InvoiceEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Monitor for the Dead Letter Queue (DLQ).
 * This consumer logs all messages that end up in the DLQ for monitoring and alerting purposes.
 * In production, you might want to:
 * - Send alerts to monitoring systems (Prometheus, Grafana, PagerDuty)
 * - Store in a dedicated database table for dashboard visualization
 * - Send email/Slack notifications to operations team
 * - Provide a UI for manual reprocessing
 */
@Service
@Slf4j
public class DeadLetterQueueMonitor {

    /**
     * Monitors the DLQ and logs failed messages.
     * This runs in a separate consumer group to not interfere with normal processing.
     *
     * @param event the failed invoice event
     */
    @KafkaListener(
            topics = "${kafka.topic.invoice-events-dlq}",
            groupId = "dlq-monitor-group"
    )
    public void monitorDLQ(InvoiceEvent event) {
        log.error("=== MESSAGE IN DEAD LETTER QUEUE ===");
        log.error("Event Type: {}", event.eventType());
        log.error("Invoice ID: {}", event.invoiceId());
        log.error("Invoice Number: {}", event.invoiceNumber());
        log.error("Client ID: {}", event.clientId());
        log.error("Client Email: {}", event.clientEmail());
        log.error("Total: {}", event.total());
        log.error("Status: {}", event.status());
        log.error("Timestamp: {}", event.timestamp());
        log.error("===================================");

        // TODO: In production, add:
        // 1. Send alert to monitoring system
        // 2. Store in database for dashboard
        // 3. Send notification to operations team
        // 4. Increment Prometheus metric

        // Example metric increment (if you add Micrometer):
        // meterRegistry.counter("dlq.messages.received",
        //     "event_type", event.eventType(),
        //     "status", event.status()
        // ).increment();

        // Example alert (if you add alerting):
        // alertService.sendAlert(
        //     "DLQ Alert",
        //     String.format("Invoice %s failed processing", event.invoiceNumber())
        // );
    }
}
