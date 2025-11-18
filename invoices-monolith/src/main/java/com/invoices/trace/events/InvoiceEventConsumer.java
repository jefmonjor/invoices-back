package com.invoices.trace.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.invoices.trace.dto.InvoiceEvent;
import com.invoices.trace.entity.AuditLog;
import com.invoices.trace.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Service;

/**
 * Consumer de eventos de factura desde Redis Streams (reemplaza Kafka)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class InvoiceEventConsumer implements StreamListener<String, MapRecord<String, String, String>> {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${spring.redis.stream.invoice-events-dlq:invoice-events-dlq}")
    private String dlqStream;

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000; // 1 second

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        String streamKey = message.getStream();
        RecordId recordId = message.getId();

        log.info("Received invoice event from stream: {}, ID: {}", streamKey, recordId);

        try {
            // Extraer datos del mensaje
            String payload = message.getValue().get("payload");

            if (payload == null || payload.isEmpty()) {
                log.error("Payload is null or empty in message ID: {}", recordId);
                return;
            }

            // Deserializar el evento
            InvoiceEvent event = objectMapper.readValue(payload, InvoiceEvent.class);

            log.info("Processing event: type={}, invoiceId={}, invoiceNumber={}",
                    event.eventType(), event.invoiceId(), event.invoiceNumber());

            // Procesar evento con reintentos
            processEventWithRetry(event);

            // Acknowledge the message by removing it from the stream (optional)
            log.info("Message processed successfully for invoice: {}", event.invoiceId());

        } catch (Exception e) {
            log.error("Failed to process event after {} retries: recordId={}, error={}",
                    MAX_RETRIES, recordId, e.getMessage(), e);

            // Send to Dead Letter Queue
            sendToDLQ(message, e);
        }
    }

    /**
     * Processes the event with automatic retry logic.
     * Implements exponential backoff for retries.
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

    /**
     * Envía el mensaje fallido a Dead Letter Queue (Redis Stream)
     */
    private void sendToDLQ(MapRecord<String, String, String> message, Exception exception) {
        try {
            log.info("Sending failed message to DLQ: {}", dlqStream);

            // Añadir información del error al mensaje
            var enrichedMessage = message.getValue();
            enrichedMessage.put("error", exception.getMessage());
            enrichedMessage.put("errorClass", exception.getClass().getName());
            enrichedMessage.put("originalStream", message.getStream());
            enrichedMessage.put("originalRecordId", message.getId().getValue());

            // Enviar a DLQ stream
            redisTemplate.opsForStream().add(dlqStream, enrichedMessage);

            log.info("Message sent to DLQ successfully");

        } catch (Exception e) {
            log.error("Failed to send message to DLQ", e);
            // En producción, podrías guardar en base de datos o enviar alerta
        }
    }
}
