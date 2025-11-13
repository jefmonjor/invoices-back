package com.invoices.trace_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

@Configuration
@EnableKafka
@Slf4j
public class KafkaConsumerConfig {

    /**
     * Configure ObjectMapper for JSON serialization/deserialization
     * Includes support for Java 8 date/time types (LocalDateTime, etc.)
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        log.info("ObjectMapper configured with JavaTimeModule for Kafka message processing");
        return mapper;
    }
}
