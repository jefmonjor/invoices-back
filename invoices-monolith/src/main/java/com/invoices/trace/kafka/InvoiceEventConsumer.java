package com.invoices.trace_service.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.invoices.trace_service.dto.InvoiceEvent;
import com.invoices.trace_service.entity.AuditLog;
import com.invoices.trace_service.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class InvoiceEventConsumer {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    private final DeadLetterQueueService deadLetterQueueService;

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000; // 1 second

    @KafkaListener(
            topics = "${kafka.topic.invoice-events}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeInvoiceEvent(InvoiceEvent event, Acknowledgment acknowledgment) {
        log.info("Received invoice event: type={}, invoiceId={}, invoiceNumber={}",
                event.eventType(), event.invoiceId(), event.invoiceNumber());

        try {
            processEventWithRetry(event);

            // Manually acknowledge the message only on success
            acknowledgment.acknowledge();
            log.info("Message processed and acknowledged successfully for invoice: {}", event.invoiceId());

        } catch (Exception e) {
            log.error("Failed to process event after {} retries: invoiceId={}, error={}",
                    MAX_RETRIES, event.invoiceId(), e.getMessage(), e);

            // Send to Dead Letter Queue
            deadLetterQueueService.sendToDLQ(event, e, MAX_RETRIES);

            // Acknowledge the message to prevent blocking the consumer
            // The message is now in DLQ for manual processing
            acknowledgment.acknowledge();
            log.info("Message sent to DLQ and acknowledged: invoiceId={}", event.invoiceId());
        }
    }

    /**
     * Processes the event with automatic retry logic.
     * Implements exponential backoff for retries.
     *
     * @param event the invoice event to process
     * @throws Exception if processing fails after all retries
     */
    private void processEventWithRetry(InvoiceEvent event) throws Exception {
        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                // Convert event to JSON string for storage
                String eventDataJson = convertEventToJson(event);

                // Create audit log entry
                AuditLog auditLog = AuditLog.builder()
                        .eventType(event.eventType())
                        .invoiceId(event.invoiceId())
                        .invoiceNumber(event.invoiceNumber())
                        .clientId(event.clientId())
                        .clientEmail(event.clientEmail())
                        .total(event.total())
                        .status(event.status())
                        .eventData(eventDataJson)
                        .build();

                // Save to database
                auditLogRepository.save(auditLog);
                log.info("Audit log saved successfully for invoice: {} (attempt {}/{})",
                        event.invoiceId(), attempt, MAX_RETRIES);

                // Success - exit retry loop
                return;

            } catch (Exception e) {
                lastException = e;
                log.warn("Failed to process event (attempt {}/{}): invoiceId={}, error={}",
                        attempt, MAX_RETRIES, event.invoiceId(), e.getMessage());

                if (attempt < MAX_RETRIES) {
                    // Wait before retrying (exponential backoff)
                    long backoffDelay = RETRY_DELAY_MS * (long) Math.pow(2, attempt - 1);
                    try {
                        Thread.sleep(backoffDelay);
                        log.debug("Retrying after {}ms backoff...", backoffDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry interrupted", ie);
                    }
                }
            }
        }

        // All retries exhausted
        throw new RuntimeException("Failed after " + MAX_RETRIES + " retries", lastException);
    }

    private String convertEventToJson(InvoiceEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            log.error("Error converting event to JSON", e);
            return String.format("{\"error\": \"Failed to serialize event\", \"eventType\": \"%s\"}", event.eventType());
        }
    }
}
