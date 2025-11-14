package com.invoices.trace_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.invoices.trace_service.dto.InvoiceEvent;
import com.invoices.trace_service.entity.AuditLog;
import com.invoices.trace_service.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for Kafka consumer using EmbeddedKafka.
 * These tests verify that the consumer correctly processes invoice events
 * from Kafka and stores them in the database.
 */
@SpringBootTest
@DirtiesContext
@EmbeddedKafka(
        partitions = 1,
        brokerProperties = {
                "listeners=PLAINTEXT://localhost:9093",
                "port=9093"
        },
        topics = {"invoice-events", "invoice-events-dlq"}
)
@TestPropertySource(
        properties = {
                "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
                "spring.kafka.consumer.auto-offset-reset=earliest",
                "kafka.topic.invoice-events=invoice-events",
                "kafka.topic.invoice-events-dlq=invoice-events-dlq"
        }
)
class InvoiceEventConsumerTest {

    @Autowired
    private KafkaTemplate<String, InvoiceEvent> kafkaTemplate;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldConsumeAndSaveInvoiceCreatedEvent() {
        // Given
        InvoiceEvent event = createInvoiceEvent(
                "INVOICE_CREATED",
                1L,
                "INV-2024-001",
                100L,
                "client@example.com",
                new BigDecimal("1500.00"),
                "PENDING"
        );

        // When
        kafkaTemplate.send("invoice-events", event);

        // Then - wait for async processing
        await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    List<AuditLog> logs = auditLogRepository.findByInvoiceId(1L);
                    assertThat(logs).hasSize(1);

                    AuditLog savedLog = logs.get(0);
                    assertThat(savedLog.getEventType()).isEqualTo("INVOICE_CREATED");
                    assertThat(savedLog.getInvoiceId()).isEqualTo(1L);
                    assertThat(savedLog.getInvoiceNumber()).isEqualTo("INV-2024-001");
                    assertThat(savedLog.getClientId()).isEqualTo(100L);
                    assertThat(savedLog.getClientEmail()).isEqualTo("client@example.com");
                    assertThat(savedLog.getTotal()).isEqualByComparingTo(new BigDecimal("1500.00"));
                    assertThat(savedLog.getStatus()).isEqualTo("PENDING");
                    assertThat(savedLog.getEventData()).contains("INVOICE_CREATED");
                    assertThat(savedLog.getCreatedAt()).isNotNull();
                });
    }

    @Test
    void shouldConsumeAndSaveInvoiceUpdatedEvent() {
        // Given
        InvoiceEvent event = createInvoiceEvent(
                "INVOICE_UPDATED",
                2L,
                "INV-2024-002",
                101L,
                "updated@example.com",
                new BigDecimal("2000.00"),
                "SENT"
        );

        // When
        kafkaTemplate.send("invoice-events", event);

        // Then
        await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    List<AuditLog> logs = auditLogRepository.findByInvoiceId(2L);
                    assertThat(logs).hasSize(1);
                    assertThat(logs.get(0).getEventType()).isEqualTo("INVOICE_UPDATED");
                    assertThat(logs.get(0).getStatus()).isEqualTo("SENT");
                });
    }

    @Test
    void shouldConsumeAndSaveInvoiceDeletedEvent() {
        // Given
        InvoiceEvent event = createInvoiceEvent(
                "INVOICE_DELETED",
                3L,
                "INV-2024-003",
                102L,
                "deleted@example.com",
                new BigDecimal("500.00"),
                "CANCELLED"
        );

        // When
        kafkaTemplate.send("invoice-events", event);

        // Then
        await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    List<AuditLog> logs = auditLogRepository.findByInvoiceId(3L);
                    assertThat(logs).hasSize(1);
                    assertThat(logs.get(0).getEventType()).isEqualTo("INVOICE_DELETED");
                });
    }

    @Test
    void shouldHandleMultipleEventsForSameInvoice() {
        // Given
        Long invoiceId = 4L;
        InvoiceEvent event1 = createInvoiceEvent(
                "INVOICE_CREATED", invoiceId, "INV-2024-004", 103L,
                "multi@example.com", new BigDecimal("1000.00"), "PENDING"
        );
        InvoiceEvent event2 = createInvoiceEvent(
                "INVOICE_UPDATED", invoiceId, "INV-2024-004", 103L,
                "multi@example.com", new BigDecimal("1200.00"), "SENT"
        );
        InvoiceEvent event3 = createInvoiceEvent(
                "INVOICE_PAID", invoiceId, "INV-2024-004", 103L,
                "multi@example.com", new BigDecimal("1200.00"), "PAID"
        );

        // When
        kafkaTemplate.send("invoice-events", event1);
        kafkaTemplate.send("invoice-events", event2);
        kafkaTemplate.send("invoice-events", event3);

        // Then
        await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    List<AuditLog> logs = auditLogRepository.findByInvoiceId(invoiceId);
                    assertThat(logs).hasSize(3);
                    assertThat(logs)
                            .extracting(AuditLog::getEventType)
                            .containsExactlyInAnyOrder("INVOICE_CREATED", "INVOICE_UPDATED", "INVOICE_PAID");
                });
    }

    @Test
    void shouldHandleEventsFromDifferentClients() {
        // Given
        InvoiceEvent event1 = createInvoiceEvent(
                "INVOICE_CREATED", 5L, "INV-2024-005", 104L,
                "client1@example.com", new BigDecimal("500.00"), "PENDING"
        );
        InvoiceEvent event2 = createInvoiceEvent(
                "INVOICE_CREATED", 6L, "INV-2024-006", 105L,
                "client2@example.com", new BigDecimal("600.00"), "PENDING"
        );

        // When
        kafkaTemplate.send("invoice-events", event1);
        kafkaTemplate.send("invoice-events", event2);

        // Then
        await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    List<AuditLog> logsClient1 = auditLogRepository.findByClientId(104L);
                    List<AuditLog> logsClient2 = auditLogRepository.findByClientId(105L);

                    assertThat(logsClient1).hasSize(1);
                    assertThat(logsClient2).hasSize(1);
                    assertThat(logsClient1.get(0).getClientEmail()).isEqualTo("client1@example.com");
                    assertThat(logsClient2.get(0).getClientEmail()).isEqualTo("client2@example.com");
                });
    }

    @Test
    void shouldStoreCompleteEventDataAsJson() throws Exception {
        // Given
        InvoiceEvent event = createInvoiceEvent(
                "INVOICE_CREATED", 7L, "INV-2024-007", 106L,
                "json@example.com", new BigDecimal("750.00"), "PENDING"
        );

        // When
        kafkaTemplate.send("invoice-events", event);

        // Then
        await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    List<AuditLog> logs = auditLogRepository.findByInvoiceId(7L);
                    assertThat(logs).hasSize(1);

                    AuditLog savedLog = logs.get(0);
                    assertThat(savedLog.getEventData()).isNotBlank();

                    // Verify we can parse the JSON back
                    InvoiceEvent parsedEvent = objectMapper.readValue(
                            savedLog.getEventData(),
                            InvoiceEvent.class
                    );
                    assertThat(parsedEvent.eventType()).isEqualTo(event.eventType());
                    assertThat(parsedEvent.invoiceId()).isEqualTo(event.invoiceId());
                    assertThat(parsedEvent.total()).isEqualByComparingTo(event.total());
                });
    }

    @Test
    void shouldHandleHighVolumeOfEvents() {
        // Given - simulate high volume
        int numberOfEvents = 50;

        // When
        for (int i = 0; i < numberOfEvents; i++) {
            InvoiceEvent event = createInvoiceEvent(
                    "INVOICE_CREATED",
                    1000L + i,
                    "INV-2024-" + (1000 + i),
                    200L + i,
                    "bulk" + i + "@example.com",
                    new BigDecimal("100.00"),
                    "PENDING"
            );
            kafkaTemplate.send("invoice-events", event);
        }

        // Then
        await()
                .atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    long count = auditLogRepository.count();
                    // Should have at least the 50 events we sent
                    // (may have more from other tests due to shared database)
                    assertThat(count).isGreaterThanOrEqualTo(numberOfEvents);
                });
    }

    @Test
    void shouldHandleEventsWithNullOptionalFields() {
        // Given - event with some null fields
        InvoiceEvent event = new InvoiceEvent(
                "INVOICE_CREATED",
                8L,
                "INV-2024-008",
                null,  // clientId can be null
                null,  // clientEmail can be null
                new BigDecimal("100.00"),
                "DRAFT",
                LocalDateTime.now()
        );

        // When
        kafkaTemplate.send("invoice-events", event);

        // Then
        await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    List<AuditLog> logs = auditLogRepository.findByInvoiceId(8L);
                    assertThat(logs).hasSize(1);
                    assertThat(logs.get(0).getClientId()).isNull();
                    assertThat(logs.get(0).getClientEmail()).isNull();
                });
    }

    @Test
    void shouldQueryEventsByType() {
        // Given
        InvoiceEvent createdEvent = createInvoiceEvent(
                "INVOICE_CREATED", 9L, "INV-2024-009", 107L,
                "type@example.com", new BigDecimal("300.00"), "PENDING"
        );
        InvoiceEvent paidEvent = createInvoiceEvent(
                "INVOICE_PAID", 10L, "INV-2024-010", 108L,
                "type2@example.com", new BigDecimal("400.00"), "PAID"
        );

        // When
        kafkaTemplate.send("invoice-events", createdEvent);
        kafkaTemplate.send("invoice-events", paidEvent);

        // Then
        await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    List<AuditLog> createdLogs = auditLogRepository.findByEventType("INVOICE_CREATED");
                    List<AuditLog> paidLogs = auditLogRepository.findByEventType("INVOICE_PAID");

                    assertThat(createdLogs).isNotEmpty();
                    assertThat(paidLogs).isNotEmpty();

                    assertThat(createdLogs.stream()
                            .anyMatch(log -> log.getInvoiceNumber().equals("INV-2024-009")))
                            .isTrue();
                    assertThat(paidLogs.stream()
                            .anyMatch(log -> log.getInvoiceNumber().equals("INV-2024-010")))
                            .isTrue();
                });
    }

    /**
     * Helper method to create invoice events for testing
     */
    private InvoiceEvent createInvoiceEvent(
            String eventType,
            Long invoiceId,
            String invoiceNumber,
            Long clientId,
            String clientEmail,
            BigDecimal total,
            String status
    ) {
        return new InvoiceEvent(
                eventType,
                invoiceId,
                invoiceNumber,
                clientId,
                clientEmail,
                total,
                status,
                LocalDateTime.now()
        );
    }
}
