package com.invoices.invoice_service.config;

import com.invoices.invoice_service.kafka.InvoiceEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuración de Kafka para el servicio de facturas
 */
@Configuration
@Slf4j
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.topic.invoice-events:invoice-events}")
    private String invoiceEventsTopic;

    /**
     * Configura el productor de Kafka para eventos de facturas
     */
    @Bean
    public ProducerFactory<String, InvoiceEvent> invoiceEventProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        configProps.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        log.info("Configurando Kafka Producer para invoice-events. Bootstrap servers: {}", bootstrapServers);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * Bean de KafkaTemplate para enviar eventos de facturas
     */
    @Bean
    public KafkaTemplate<String, InvoiceEvent> kafkaTemplate() {
        return new KafkaTemplate<>(invoiceEventProducerFactory());
    }

    /**
     * Crea el topic de eventos de facturas si no existe
     */
    @Bean
    public NewTopic invoiceEventsTopic() {
        log.info("Configurando topic de Kafka: {}", invoiceEventsTopic);
        return TopicBuilder.name(invoiceEventsTopic)
                .partitions(3)
                .replicas(1)
                .compact()
                .build();
    }
}
