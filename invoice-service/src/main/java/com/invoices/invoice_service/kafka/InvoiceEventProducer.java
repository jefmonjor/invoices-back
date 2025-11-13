package com.invoices.invoice_service.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Servicio que produce eventos de factura hacia Kafka
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceEventProducer {

    private final KafkaTemplate<String, InvoiceEvent> kafkaTemplate;

    @Value("${spring.kafka.topic.invoice-events:invoice-events}")
    private String invoiceEventsTopic;

    /**
     * Envía un evento de factura creada
     */
    public void sendInvoiceCreated(InvoiceEvent event) {
        log.info("Enviando evento INVOICE_CREATED para factura: {} al topic: {}", event.invoiceNumber(), invoiceEventsTopic);
        sendEvent(event);
    }

    /**
     * Envía un evento de factura actualizada
     */
    public void sendInvoiceUpdated(InvoiceEvent event) {
        log.info("Enviando evento INVOICE_UPDATED para factura: {} al topic: {}", event.invoiceNumber(), invoiceEventsTopic);
        sendEvent(event);
    }

    /**
     * Envía un evento de factura pagada
     */
    public void sendInvoicePaid(InvoiceEvent event) {
        log.info("Enviando evento INVOICE_PAID para factura: {} al topic: {}", event.invoiceNumber(), invoiceEventsTopic);
        sendEvent(event);
    }

    /**
     * Envía un evento de factura cancelada
     */
    public void sendInvoiceCancelled(InvoiceEvent event) {
        log.info("Enviando evento INVOICE_CANCELLED para factura: {} al topic: {}", event.invoiceNumber(), invoiceEventsTopic);
        sendEvent(event);
    }

    /**
     * Método privado para enviar eventos a Kafka
     */
    private void sendEvent(InvoiceEvent event) {
        try {
            kafkaTemplate.send(invoiceEventsTopic, event.invoiceNumber(), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Error al enviar evento de factura {} a Kafka: {}",
                                    event.invoiceNumber(), ex.getMessage());
                        } else {
                            log.debug("Evento enviado exitosamente para factura: {} con offset: {}",
                                    event.invoiceNumber(),
                                    result.getRecordMetadata().offset());
                        }
                    });
        } catch (Exception e) {
            log.error("Error inesperado al enviar evento de factura {} a Kafka", event.invoiceNumber(), e);
        }
    }
}
