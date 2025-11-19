package com.invoices.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.invoices.security.JwtAuthenticationFilter;
import com.invoices.security.JwtService;
import io.minio.MinioClient;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/**
 * Test configuration that provides mock/test versions of beans
 * that require external services (Redis, MinIO, Security) during tests.
 */
@TestConfiguration
@Profile("test")
@EnableAutoConfiguration(exclude = {
        RedisAutoConfiguration.class,
        RedisRepositoriesAutoConfiguration.class,
        FlywayAutoConfiguration.class,
        DataSourceAutoConfiguration.class
})
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

    /**
     * Mock JwtService for tests that require JWT functionality.
     */
    @Bean
    @Primary
    public JwtService testJwtService() {
        return Mockito.mock(JwtService.class);
    }

    /**
     * Mock JwtAuthenticationFilter for tests that require authentication.
     */
    @Bean
    @Primary
    public JwtAuthenticationFilter testJwtAuthenticationFilter() {
        return Mockito.mock(JwtAuthenticationFilter.class);
    }

    /**
     * Provides an in-memory UserDetailsService for security tests.
     */
    @Bean
    @Primary
    public UserDetailsService testUserDetailsService() {
        UserDetails testUser = User.builder()
                .username("testuser")
                .password("password")
                .roles("USER")
                .build();

        return new InMemoryUserDetailsManager(testUser);
    }
}
