package com.invoices.trace.config;

import com.invoices.trace.infrastructure.events.RedisInvoiceEventConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;

import java.time.Duration;

/**
 * Configuración de Redis Streams para consumir eventos
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
@Profile("!test")  // Don't run in test profile
public class RedisStreamConfig {

    private final RedisInvoiceEventConsumer invoiceEventConsumer;

    @Value("${spring.redis.stream.invoice-events:invoice-events}")
    private String invoiceEventsStream;

    @Value("${spring.redis.stream.consumer-group:trace-group}")
    private String consumerGroup;

    @Value("${spring.redis.stream.consumer-name:trace-consumer}")
    private String consumerName;

    /**
     * Container que escucha los streams de Redis
     */
    @Bean
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamMessageListenerContainer(
            RedisConnectionFactory connectionFactory) {

        var options = StreamMessageListenerContainer
                .StreamMessageListenerContainerOptions
                .builder()
                .pollTimeout(Duration.ofSeconds(2))
                .build();

        var container = StreamMessageListenerContainer.create(connectionFactory, options);

        log.info("Redis Stream Listener Container configurado para stream: {} con consumer group: {}",
                invoiceEventsStream, consumerGroup);

        return container;
    }

    /**
     * Suscripción al stream de eventos de facturas
     */
    @Bean
    public Subscription invoiceEventsSubscription(
            StreamMessageListenerContainer<String, MapRecord<String, String, String>> container) {

        try {
            // Crear consumer group si no existe
            createConsumerGroupIfNotExists();

            // Suscribirse al stream
            var subscription = container.receive(
                    Consumer.from(consumerGroup, consumerName),
                    StreamOffset.create(invoiceEventsStream, ReadOffset.lastConsumed()),
                    invoiceEventConsumer
            );

            container.start();

            log.info("Subscribed to Redis Stream: {} as consumer: {} in group: {}",
                    invoiceEventsStream, consumerName, consumerGroup);

            return subscription;

        } catch (Exception e) {
            log.error("Error al suscribirse al stream de Redis: {}", invoiceEventsStream, e);
            throw new RuntimeException("Failed to subscribe to Redis stream", e);
        }
    }

    /**
     * Crea el consumer group si no existe
     */
    private void createConsumerGroupIfNotExists() {
        // Esta operación se puede hacer manualmente o con un script de inicialización
        // Redis creará automáticamente el stream cuando se envíe el primer mensaje
        log.info("Consumer group '{}' will be created automatically on first message", consumerGroup);
    }
}
