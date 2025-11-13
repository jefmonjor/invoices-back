package com.invoices.trace_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.invoices.trace_service.dto.InvoiceEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.FixedBackOff;

import java.util.List;

@Configuration
@EnableKafka
@Slf4j
public class KafkaConsumerConfig {

    @Value("${kafka.topic.invoice-events-dlq:invoice-events-dlq}")
    private String dlqTopic;

    /**
     * Configure ObjectMapper for JSON serialization/deserialization
     * Includes support for Java 8 date/time types (LocalDateTime, etc.)
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        log.info("ObjectMapper configured with JavaTimeModule for Kafka message processing");
        return mapper;
    }

    /**
     * Configure error handler with exponential backoff and DLQ support.
     * After max retries, failed messages are sent to the Dead Letter Queue.
     */
    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<String, InvoiceEvent> kafkaTemplate) {
        // Configure exponential backoff: 1s, 2s, 4s (3 retries max)
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(3);
        backOff.setInitialInterval(1000L);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(10000L);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                (consumerRecord, exception) -> {
                    // This is called after all retries have been exhausted
                    log.error("Message processing failed after retries. Sending to DLQ. Topic: {}, Partition: {}, Offset: {}",
                            consumerRecord.topic(), consumerRecord.partition(), consumerRecord.offset(), exception);

                    try {
                        // Send to Dead Letter Queue
                        InvoiceEvent failedEvent = (InvoiceEvent) consumerRecord.value();
                        kafkaTemplate.send(dlqTopic, failedEvent);
                        log.info("Failed message sent to DLQ: {}", dlqTopic);
                    } catch (Exception e) {
                        log.error("Failed to send message to DLQ", e);
                        // In production, you might want to:
                        // 1. Store in a database table for manual review
                        // 2. Send alert to monitoring system
                        // 3. Write to a file for later processing
                    }
                },
                backOff
        );

        // Don't retry certain exceptions (e.g., deserialization errors)
        errorHandler.addNotRetryableExceptions(
                org.springframework.kafka.support.serializer.DeserializationException.class,
                IllegalArgumentException.class
        );

        log.info("Kafka error handler configured with exponential backoff and DLQ support");
        return errorHandler;
    }
}

