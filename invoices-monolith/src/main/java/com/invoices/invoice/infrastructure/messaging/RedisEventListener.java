package com.invoices.invoice.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.invoices.invoice.infrastructure.messaging.dto.InvoiceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisEventListener implements MessageListener {

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            InvoiceEvent event = objectMapper.readValue(message.getBody(), InvoiceEvent.class);
            log.debug("Received Redis event: {}", event);

            // Forward to WebSocket topic
            // Topic: /topic/invoice/{invoiceId}/status
            // Send to invoice specific topic
            String invoiceDestination = "/topic/invoice/" + event.getInvoiceId() + "/status";
            messagingTemplate.convertAndSend(invoiceDestination, event);

            // Send to company topic (for list view updates)
            String companyDestination = "/topic/company/" + event.getCompanyId() + "/invoices";
            messagingTemplate.convertAndSend(companyDestination, event);

        } catch (IOException e) {
            log.error("Error parsing Redis message", e);
        }
    }
}
