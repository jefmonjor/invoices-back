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

    @KafkaListener(
            topics = "${kafka.topic.invoice-events}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeInvoiceEvent(InvoiceEvent event, Acknowledgment acknowledgment) {
        try {
            log.info("Received invoice event: type={}, invoiceId={}, invoiceNumber={}",
                    event.eventType(), event.invoiceId(), event.invoiceNumber());

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
            log.info("Audit log saved successfully for invoice: {}", event.invoiceId());

            // Manually acknowledge the message
            acknowledgment.acknowledge();
            log.debug("Message acknowledged for invoice: {}", event.invoiceId());

        } catch (Exception e) {
            // Log the error but don't throw exception to avoid infinite retries
            // The message will not be acknowledged and Kafka will reprocess it based on retry configuration
            log.error("Error processing invoice event: invoiceId={}, error={}",
                    event.invoiceId(), e.getMessage(), e);

            // In production, you might want to:
            // 1. Send to a dead letter queue (DLQ)
            // 2. Implement exponential backoff
            // 3. Alert monitoring systems
            // For now, we acknowledge to prevent infinite loop (adjust based on your requirements)
            acknowledgment.acknowledge();
        }
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
