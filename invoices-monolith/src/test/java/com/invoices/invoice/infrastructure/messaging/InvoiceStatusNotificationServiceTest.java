package com.invoices.invoice.infrastructure.messaging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * Unit test for InvoiceStatusNotificationService using mocks.
 * 
 * This test verifies the business logic without requiring actual WebSocket
 * connectivity.
 * Preferred over integration tests due to WebSocket testing limitations in
 * embedded containers.
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings({ "unchecked", "rawtypes" }) // ArgumentCaptor with Map generics
class InvoiceStatusNotificationServiceTest {

        @Mock
        private SimpMessagingTemplate messagingTemplate;

        @InjectMocks
        private InvoiceStatusNotificationService notificationService;

        @Test
        void shouldSendStatusNotificationToCorrectTopic() {
                // Given
                Long invoiceId = 123L;
                String status = "processing";

                // When
                notificationService.notifyStatus(invoiceId, status);

                // Then
                ArgumentCaptor<Map<String, Object>> messageCaptor = ArgumentCaptor.forClass(Map.class);
                verify(messagingTemplate).convertAndSend(
                                eq("/topic/invoice/" + invoiceId + "/status"),
                                messageCaptor.capture());

                Map<String, Object> sentMessage = messageCaptor.getValue();
                assertThat(sentMessage).isNotNull();
                assertThat(sentMessage.get("invoiceId")).isEqualTo(invoiceId);
                assertThat(sentMessage.get("status")).isEqualTo(status);
                assertThat(sentMessage.get("timestamp")).isNotNull();
        }

        @Test
        void shouldSendStatusWithTxNotification() {
                // Given
                Long invoiceId = 456L;
                String status = "accepted";
                String txId = "TX-12345";
                String message = "Invoice accepted by VeriFactu";

                // When
                notificationService.notifyStatusWithTx(invoiceId, status, txId, message);

                // Then
                ArgumentCaptor<Map<String, Object>> messageCaptor = ArgumentCaptor.forClass(Map.class);
                verify(messagingTemplate).convertAndSend(
                                eq("/topic/invoice/" + invoiceId + "/status"),
                                messageCaptor.capture());

                Map<String, Object> sentMessage = messageCaptor.getValue();
                assertThat(sentMessage).isNotNull();
                assertThat(sentMessage.get("invoiceId")).isEqualTo(invoiceId);
                assertThat(sentMessage.get("status")).isEqualTo(status);
                assertThat(sentMessage.get("txId")).isEqualTo(txId);
                assertThat(sentMessage.get("message")).isEqualTo(message);
                assertThat(sentMessage.get("timestamp")).isNotNull();
        }

        @Test
        void shouldHandleNullTxIdGracefully() {
                // Given
                Long invoiceId = 789L;
                String status = "pending";

                // When
                notificationService.notifyStatusWithTx(invoiceId, status, null, null);

                // Then - Should not throw exception
                ArgumentCaptor<Map<String, Object>> messageCaptor = ArgumentCaptor.forClass(Map.class);
                verify(messagingTemplate).convertAndSend(
                                eq("/topic/invoice/" + invoiceId + "/status"),
                                messageCaptor.capture());

                Map<String, Object> sentMessage = messageCaptor.getValue();
                assertThat(sentMessage).isNotNull();
                assertThat(sentMessage.get("invoiceId")).isEqualTo(invoiceId);
                assertThat(sentMessage.get("status")).isEqualTo(status);
        }

        @Test
        void shouldIncludeTimestampInAllNotifications() {
                // Given
                Long invoiceId = 999L;
                String status = "completed";

                // When
                notificationService.notifyStatus(invoiceId, status);

                // Then
                ArgumentCaptor<Map<String, Object>> messageCaptor = ArgumentCaptor.forClass(Map.class);
                verify(messagingTemplate).convertAndSend(
                                eq("/topic/invoice/" + invoiceId + "/status"),
                                messageCaptor.capture());

                Map<String, Object> sentMessage = messageCaptor.getValue();
                assertThat(sentMessage.get("timestamp"))
                                .isNotNull()
                                .isInstanceOf(String.class)
                                .asString()
                                .contains("2025"); // Verify it's a valid timestamp string
        }

        @Test
        void shouldSendToInvoiceSpecificTopic() {
                // Given
                Long invoiceId1 = 111L;
                Long invoiceId2 = 222L;

                // When
                notificationService.notifyStatus(invoiceId1, "draft");
                notificationService.notifyStatus(invoiceId2, "sent");

                // Then - Each notification goes to its own topic
                verify(messagingTemplate).convertAndSend(
                                eq("/topic/invoice/111/status"),
                                org.mockito.ArgumentMatchers.any(Map.class));
                verify(messagingTemplate).convertAndSend(
                                eq("/topic/invoice/222/status"),
                                org.mockito.ArgumentMatchers.any(Map.class));
        }
}
