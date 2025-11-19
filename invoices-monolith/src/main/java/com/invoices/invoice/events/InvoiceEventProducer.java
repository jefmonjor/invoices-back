package com.invoices.invoice.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.invoices.trace.domain.events.InvoiceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Servicio que produce eventos de factura hacia Redis Streams (reemplaza Kafka)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceEventProducer {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${spring.redis.stream.invoice-events:invoice-events}")
    private String invoiceEventsStream;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    /**
     * Envía un evento de factura creada
     */
    public void sendInvoiceCreated(InvoiceEvent event) {
        log.info("Enviando evento INVOICE_CREATED para factura: {} al stream: {}",
                event.getInvoiceNumber(), invoiceEventsStream);
        sendEvent(event);
    }

    /**
     * Envía un evento de factura actualizada
     */
    public void sendInvoiceUpdated(InvoiceEvent event) {
        log.info("Enviando evento INVOICE_UPDATED para factura: {} al stream: {}",
                event.getInvoiceNumber(), invoiceEventsStream);
        sendEvent(event);
    }

    /**
     * Envía un evento de factura pagada
     */
    public void sendInvoicePaid(InvoiceEvent event) {
        log.info("Enviando evento INVOICE_PAID para factura: {} al stream: {}",
                event.getInvoiceNumber(), invoiceEventsStream);
        sendEvent(event);
    }

    /**
     * Envía un evento de factura cancelada
     */
    public void sendInvoiceCancelled(InvoiceEvent event) {
        log.info("Enviando evento INVOICE_CANCELLED para factura: {} al stream: {}",
                event.getInvoiceNumber(), invoiceEventsStream);
        sendEvent(event);
    }

    /**
     * Método privado para enviar eventos a Redis Streams
     */
    private void sendEvent(InvoiceEvent event) {
        try {
            // Convertir el evento a un Map para Redis Streams
            Map<String, String> eventData = new HashMap<>();
            eventData.put("eventType", event.getEventType());
            eventData.put("invoiceId", String.valueOf(event.getInvoiceId()));
            eventData.put("invoiceNumber", event.getInvoiceNumber());
            eventData.put("clientId", String.valueOf(event.getClientId()));
            eventData.put("clientEmail", event.getClientEmail());
            eventData.put("total", event.getTotal().toString());
            eventData.put("status", event.getStatus());
            eventData.put("timestamp", event.getTimestamp().toString());

            // Serializar el evento completo como JSON para tenerlo completo
            eventData.put("payload", objectMapper.writeValueAsString(event));

            // Enviar al stream de Redis
            RecordId recordId = redisTemplate.opsForStream()
                    .add(StreamRecords.newRecord()
                            .in(invoiceEventsStream)
                            .ofStrings(eventData));

            log.debug("Evento enviado exitosamente para factura: {} con ID: {}",
                    event.getInvoiceNumber(), recordId);

        } catch (JsonProcessingException e) {
            log.error("Error al serializar evento de factura {} a JSON", event.getInvoiceNumber(), e);
        } catch (Exception e) {
            log.error("Error inesperado al enviar evento de factura {} a Redis Streams",
                    event.getInvoiceNumber(), e);
        }
    }
}
