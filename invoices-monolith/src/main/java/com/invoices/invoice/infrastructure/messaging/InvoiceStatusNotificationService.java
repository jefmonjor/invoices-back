package com.invoices.invoice.infrastructure.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceStatusNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void notifyStatus(Long invoiceId, String status) {
        String destination = "/topic/invoice/" + invoiceId + "/status";
        Map<String, Object> payload = Map.of(
                "invoiceId", invoiceId,
                "status", status,
                "timestamp", LocalDateTime.now().toString());

        log.info("Sending WebSocket notification to {}: {}", destination, payload);
        messagingTemplate.convertAndSend(destination, payload);
    }

    public void notifyStatusWithTx(Long invoiceId, String status, String txId, String message) {
        String destination = "/topic/invoice/" + invoiceId + "/status";
        Map<String, Object> payload = Map.of(
                "invoiceId", invoiceId,
                "status", status,
                "txId", txId != null ? txId : "",
                "message", message != null ? message : "",
                "timestamp", LocalDateTime.now().toString());

        log.info("Sending WebSocket notification to {}: {}", destination, payload);
        messagingTemplate.convertAndSend(destination, payload);
    }
}
