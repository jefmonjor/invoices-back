package com.invoices.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.minio.MinioClient;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * Test configuration that provides mock/test versions of beans
 * that require external services (Redis, MinIO) during tests.
 */
@TestConfiguration
@Profile("test")
public class TestConfig {

    /**
     * Mock MinioClient to prevent S3/MinIO connection attempts during tests.
     */
    @Bean
    @Primary
    public MinioClient testMinioClient() {
        return Mockito.mock(MinioClient.class);
    }

    /**
     * ObjectMapper for JSON serialization in tests.
     * Since RedisConfig won't load in test profile, we provide this here.
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}
