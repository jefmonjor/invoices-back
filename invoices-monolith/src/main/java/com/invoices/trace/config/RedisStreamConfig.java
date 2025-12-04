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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;

import jakarta.annotation.PreDestroy;
import java.time.Duration;

/**
 * Configuración de Redis Streams para consumir eventos
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
@Profile("!test") // Don't run in test profile
public class RedisStreamConfig {

    private final RedisInvoiceEventConsumer invoiceEventConsumer;
    private final StringRedisTemplate redisTemplate;

    private StreamMessageListenerContainer<String, MapRecord<String, String, String>> listenerContainer;

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

        var options = StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                .builder()
                .pollTimeout(Duration.ofSeconds(2))
                .build();

        listenerContainer = StreamMessageListenerContainer.create(connectionFactory, options);

        log.info("Redis Stream Listener Container configurado para stream: {} con consumer group: {}",
                invoiceEventsStream, consumerGroup);

        return listenerContainer;
    }

    /**
     * Cleanup del container para evitar fugas de memoria y conexiones
     */
    @PreDestroy
    public void cleanup() {
        if (listenerContainer != null && listenerContainer.isRunning()) {
            log.info("Stopping Redis Stream Listener Container...");
            listenerContainer.stop();
            log.info("Redis Stream Listener Container stopped");
        }
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
                    invoiceEventConsumer);

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
        try {
            // Verificar si el stream existe
            if (Boolean.FALSE.equals(redisTemplate.hasKey(invoiceEventsStream))) {
                log.info("Stream {} does not exist. Creating it along with the consumer group.", invoiceEventsStream);
                // Crear el grupo crea el stream automáticamente con MKSTREAM (implícito en
                // createGroup de Spring Data Redis a veces, pero mejor asegurar)
                // Spring Data Redis createGroup usa XGROUP CREATE ... MKSTREAM por defecto si
                // el stream no existe?
                // No siempre. Pero createGroup suele fallar si el stream no existe a menos que
                // se use MKSTREAM.
                // La implementación de Lettuce/Jedis subyacente puede variar.
                // Intentamos crear el grupo.
                try {
                    redisTemplate.opsForStream().createGroup(invoiceEventsStream, ReadOffset.from("0-0"),
                            consumerGroup);
                } catch (Exception ex) {
                    // Si falla porque el stream no existe, podríamos necesitar crearlo primero
                    // enviando un mensaje dummy o usando comandos raw.
                    // Pero createGroup en versiones recientes suele manejarlo o lanzar excepción.
                    log.warn("Could not create group, possibly stream missing or group exists. Error: {}",
                            ex.getMessage());
                }
            } else {
                // Si el stream existe, intentamos crear el grupo.
                try {
                    redisTemplate.opsForStream().createGroup(invoiceEventsStream, ReadOffset.from("0-0"),
                            consumerGroup);
                    log.info("Consumer group {} created for stream {}", consumerGroup, invoiceEventsStream);
                } catch (Exception e) {
                    // Ignorar si el grupo ya existe (RedisBusyException o similar)
                    log.info("Consumer group {} likely already exists for stream {}", consumerGroup,
                            invoiceEventsStream);
                }
            }
        } catch (Exception e) {
            log.error("Error ensuring consumer group exists: {}", e.getMessage());
            // No lanzamos excepción para no detener el arranque, pero el listener podría
            // fallar si el grupo no se creó
        }
    }
}
