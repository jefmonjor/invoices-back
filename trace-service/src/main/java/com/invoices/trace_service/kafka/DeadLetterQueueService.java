package com.invoices.trace_service.kafka;

import com.invoices.trace_service.dto.InvoiceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Service responsible for sending failed messages to the Dead Letter Queue (DLQ).
 * When a message cannot be processed after multiple retries, it is sent to the DLQ
 * for manual investigation and reprocessing.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DeadLetterQueueService {

    private final KafkaTemplate<String, InvoiceEvent> kafkaTemplate;

    @Value("${kafka.topic.invoice-events-dlq}")
    private String dlqTopic;

    /**
     * Sends a failed invoice event to the Dead Letter Queue.
     *
     * @param event the event that failed to process
     * @param exception the exception that caused the failure
     */
    public void sendToDLQ(InvoiceEvent event, Exception exception) {
        log.warn("Sending message to DLQ: invoiceId={}, eventType={}, reason={}",
                event.invoiceId(), event.eventType(), exception.getMessage());

        try {
            CompletableFuture<SendResult<String, InvoiceEvent>> future =
                    kafkaTemplate.send(dlqTopic, event.invoiceId().toString(), event);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Successfully sent message to DLQ: invoiceId={}, offset={}",
                            event.invoiceId(),
                            result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to send message to DLQ: invoiceId={}, error={}",
                            event.invoiceId(), ex.getMessage(), ex);
                    // In production, you might want to:
                    // 1. Store in a database table for manual retry
                    // 2. Send alert to monitoring system
                    // 3. Write to file system as last resort
                }
            });
        } catch (Exception e) {
            log.error("Critical error while sending to DLQ: invoiceId={}, error={}",
                    event.invoiceId(), e.getMessage(), e);
            // This is a critical failure - the message couldn't be processed AND couldn't be sent to DLQ
            // Consider alerting operations team
        }
    }

    /**
     * Sends a failed event with additional error details.
     *
     * @param event the event that failed
     * @param exception the exception
     * @param retryCount number of retries attempted
     */
    public void sendToDLQ(InvoiceEvent event, Exception exception, int retryCount) {
        log.warn("Sending message to DLQ after {} retries: invoiceId={}, eventType={}, reason={}",
                retryCount, event.invoiceId(), event.eventType(), exception.getMessage());

        sendToDLQ(event, exception);
    }
}
