package com.invoices.trace.infrastructure.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.invoices.trace.domain.events.InvoiceEvent;
import com.invoices.trace.domain.services.RetryPolicy;
import com.invoices.trace.domain.usecases.RecordAuditLogUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Redis Streams consumer for invoice events (Clean Architecture adapter).
 * Implements event consumption using domain use cases and retry policy.
 */
@Service
@Slf4j
public class RedisInvoiceEventConsumer implements StreamListener<String, MapRecord<String, String, String>> {

    private final RecordAuditLogUseCase recordAuditLogUseCase;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RetryPolicy retryPolicy;

    @Value("${spring.redis.stream.invoice-events-dlq:invoice-events-dlq}")
    private String dlqStream;

    public RedisInvoiceEventConsumer(
            RecordAuditLogUseCase recordAuditLogUseCase,
            ObjectMapper objectMapper,
            RedisTemplate<String, Object> redisTemplate) {
        this.recordAuditLogUseCase = recordAuditLogUseCase;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
        this.retryPolicy = RetryPolicy.defaultPolicy(); // 3 retries, 1s initial delay
    }

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        String streamKey = message.getStream();
        RecordId recordId = message.getId();

        log.info("Received invoice event from stream: {}, ID: {}", streamKey, recordId);

        try {
            // Extract payload from message
            String payload = message.getValue().get("payload");

            if (payload == null || payload.isEmpty()) {
                log.error("Payload is null or empty in message ID: {}", recordId);
                return;
            }

            // Deserialize the event to domain InvoiceEvent
            InvoiceEvent event = deserializeEvent(payload);

            log.info("Processing event: type={}, invoiceId={}, invoiceNumber={}",
                    event.getEventType(), event.getInvoiceId(), event.getInvoiceNumber());

            // Process event with retry logic using domain RetryPolicy
            processEventWithRetry(event, payload);

            log.info("Message processed successfully for invoice: {}", event.getInvoiceId());

        } catch (Exception e) {
            log.error("Failed to process event after {} retries: recordId={}, error={}",
                    retryPolicy.getMaxRetries(), recordId, e.getMessage(), e);

            // Send to Dead Letter Queue
            sendToDLQ(message, e);
        }
    }

    /**
     * Deserializes JSON payload to domain InvoiceEvent.
     */
    private InvoiceEvent deserializeEvent(String payload) throws JsonProcessingException {
        // First deserialize to a DTO record for JSON parsing
        InvoiceEventDto dto = objectMapper.readValue(payload, InvoiceEventDto.class);

        // Convert DTO to domain event
        return new InvoiceEvent(
                dto.companyId(),
                dto.eventType(),
                dto.invoiceId(),
                dto.invoiceNumber(),
                dto.clientId(),
                dto.clientEmail(),
                dto.total(),
                dto.status(),
                dto.timestamp());
    }

    /**
     * Processes the event with automatic retry logic using domain RetryPolicy.
     */
    private void processEventWithRetry(InvoiceEvent event, String eventDataJson) throws Exception {
        retryPolicy.executeVoid(() -> {
            // Use the domain use case to record the audit log
            recordAuditLogUseCase.execute(event, eventDataJson);
            log.info("Audit log saved successfully for invoice: {}", event.getInvoiceId());
        });
    }

    /**
     * Sends failed message to Dead Letter Queue (Redis Stream).
     */
    private void sendToDLQ(MapRecord<String, String, String> message, Exception exception) {
        try {
            log.info("Sending failed message to DLQ: {}", dlqStream);

            // Add error information to the message
            var enrichedMessage = message.getValue();
            enrichedMessage.put("error", exception.getMessage());
            enrichedMessage.put("errorClass", exception.getClass().getName());
            enrichedMessage.put("originalStream", message.getStream());
            enrichedMessage.put("originalRecordId", message.getId().getValue());

            // Send to DLQ stream
            redisTemplate.opsForStream().add(dlqStream, enrichedMessage);

            log.info("Message sent to DLQ successfully");

        } catch (Exception e) {
            log.error("Failed to send message to DLQ", e);
            // In production, you could save to database or send alert
        }
    }

    /**
     * Internal DTO record for JSON deserialization.
     * This is infrastructure-specific and doesn't pollute the domain.
     */
    private record InvoiceEventDto(
            Long companyId,
            String eventType,
            Long invoiceId,
            String invoiceNumber,
            Long clientId,
            String clientEmail,
            BigDecimal total,
            String status,
            LocalDateTime timestamp) {
    }
}
