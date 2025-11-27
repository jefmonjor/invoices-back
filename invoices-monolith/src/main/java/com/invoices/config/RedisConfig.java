package com.invoices.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis configuration for event streaming (replaces Kafka).
 * Consolidated configuration for all modules (invoice, trace).
 */
@Configuration
@Slf4j
@Profile("!test") // Don't run in test profile
public class RedisConfig {

    @Value("${spring.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private int redisPort;

    @Value("${spring.redis.password:}")
    private String redisPassword;

    @Value("${spring.redis.ssl:false}")
    private boolean redisSsl;

    /**
     * Configures the Redis connection.
     */
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(redisHost);
        redisConfig.setPort(redisPort);

        if (redisPassword != null && !redisPassword.isEmpty()) {
            redisConfig.setPassword(redisPassword);
        }

        LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfigBuilder = LettuceClientConfiguration
                .builder();

        if (redisSsl) {
            clientConfigBuilder.useSsl();
        }

        LettuceClientConfiguration clientConfig = clientConfigBuilder.build();

        LettuceConnectionFactory factory = new LettuceConnectionFactory(redisConfig, clientConfig);

        log.info("Configuring Redis connection: {}:{} (SSL: {})", redisHost, redisPort, redisSsl);

        return factory;
    }

    /**
     * Redis template for general operations.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Configure serializers
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // ObjectMapper with Java 8 DateTime support
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();

        log.info("RedisTemplate configured successfully");

        return template;
    }

    /**
     * ObjectMapper for JSON serialization/deserialization.
     * Used by trace module for Redis message processing.
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        log.info("ObjectMapper configured with JavaTimeModule for Redis message processing");
        return mapper;
    }

    /**
     * Redis Message Listener Container for processing Redis Pub/Sub messages.
     */
    @Bean
    public org.springframework.data.redis.listener.RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            com.invoices.invoice.infrastructure.messaging.RedisEventListener redisEventListener) {

        org.springframework.data.redis.listener.RedisMessageListenerContainer container = new org.springframework.data.redis.listener.RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        // Subscribe to tenant events
        container.addMessageListener(redisEventListener,
                new org.springframework.data.redis.listener.PatternTopic("tenant:*:events"));

        log.info("RedisMessageListenerContainer configured with listener for 'tenant:*:events'");

        return container;
    }
}
