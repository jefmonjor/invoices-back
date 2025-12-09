package com.invoices.config.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Health indicator for Redis service.
 * Checks if Redis is reachable and responsive.
 */
@Component("redisCustom")
@RequiredArgsConstructor
@Slf4j
public class RedisHealthIndicator implements HealthIndicator {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Health health() {
        try {
            RedisConnectionFactory connectionFactory = redisTemplate.getConnectionFactory();
            if (connectionFactory == null) {
                return Health.down()
                        .withDetail("error", "No connection factory available")
                        .build();
            }

            // Try to ping Redis
            String pong = connectionFactory.getConnection().ping();

            if ("PONG".equals(pong)) {
                return Health.up()
                        .withDetail("response", pong)
                        .withDetail("streams", getStreamInfo())
                        .build();
            } else {
                return Health.down()
                        .withDetail("error", "Unexpected response: " + pong)
                        .build();
            }

        } catch (Exception e) {
            log.error("Redis health check failed", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }

    private String getStreamInfo() {
        try {
            // Check if VeriFactu queue exists
            Long queueLength = redisTemplate.opsForStream().size("verifactu-queue");
            return "verifactu-queue: " + (queueLength != null ? queueLength : 0) + " messages";
        } catch (Exception e) {
            return "Unable to get stream info";
        }
    }
}
