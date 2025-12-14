package com.invoices.invoice.infrastructure.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * No-op implementation of invoice status notification service.
 * Replaces WebSocket-based notifications with simple logging.
 * 
 * The frontend uses polling instead of WebSockets for MVP simplicity.
 * Status updates are visible when frontend polls the invoice API.
 */
@Service
@Slf4j
public class InvoiceStatusNotificationService {

    public void notifyStatus(Long invoiceId, String status) {
        log.info("[Status Update] Invoice {} -> {} (timestamp: {})",
                invoiceId, status, LocalDateTime.now());
    }

    public void notifyStatusWithTx(Long invoiceId, String status, String txId, String message) {
        log.info("[Status Update] Invoice {} -> {} (txId: {}, message: {}, timestamp: {})",
                invoiceId, status,
                txId != null ? txId : "N/A",
                message != null ? message : "N/A",
                LocalDateTime.now());
    }
}
