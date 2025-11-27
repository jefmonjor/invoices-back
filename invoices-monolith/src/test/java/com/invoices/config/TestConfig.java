package com.invoices.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.invoices.security.JwtAuthenticationFilter;
import com.invoices.security.JwtUtil;
import io.minio.MinioClient;
import org.mockito.Mockito;

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
     * Mock MinioProperties for tests.
     */
    @Bean
    public com.invoices.document.config.MinioConfig.MinioProperties minioProperties() {
        com.invoices.document.config.MinioConfig.MinioProperties properties = new com.invoices.document.config.MinioConfig.MinioProperties();
        properties.setBucketName("test-bucket");
        properties.setEndpoint("http://localhost:9000");
        properties.setAccessKey("test");
        properties.setSecretKey("test");
        return properties;
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
     * Mock JwtUtil for tests that require JWT functionality.
     */
    @Bean
    @Primary
    public JwtUtil testJwtUtil() {
        return Mockito.mock(JwtUtil.class);
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

    /**
     * Mock CompanySecurityInterceptor for multi-company tests.
     */
    @Bean
    @Primary
    public com.invoices.security.infrastructure.interceptor.CompanySecurityInterceptor testCompanySecurityInterceptor() {
        return Mockito.mock(com.invoices.security.infrastructure.interceptor.CompanySecurityInterceptor.class);
    }

    /**
     * Mock RedisTemplate for event streaming tests.
     */
    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public org.springframework.data.redis.core.RedisTemplate<String, Object> testRedisTemplate() {
        org.springframework.data.redis.core.RedisTemplate<String, Object> mockTemplate = Mockito
                .mock(org.springframework.data.redis.core.RedisTemplate.class);
        org.springframework.data.redis.core.StreamOperations<String, Object, Object> mockStreamOps = Mockito
                .mock(org.springframework.data.redis.core.StreamOperations.class);
        org.springframework.data.redis.core.ValueOperations<String, Object> mockValueOps = Mockito
                .mock(org.springframework.data.redis.core.ValueOperations.class);

        Mockito.when(mockTemplate.opsForStream()).thenReturn(mockStreamOps);
        Mockito.when(mockTemplate.opsForValue()).thenReturn(mockValueOps);

        return mockTemplate;
    }

    /**
     * Mock JavaMailSender for email service tests.
     */
    @Bean
    @Primary
    public org.springframework.mail.javamail.JavaMailSender testJavaMailSender() {
        return Mockito.mock(org.springframework.mail.javamail.JavaMailSender.class);
    }
}
