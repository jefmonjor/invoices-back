package com.invoices.invoice.infrastructure.persistence.repositories;

import com.invoices.invoice.domain.ports.DLQMonitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Implementation of DLQMonitorService using Redis.
 *
 * Monitors Dead Letter Queue metrics stored in Redis.
 * Provides abstraction for Redis operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DLQMonitorServiceImpl implements DLQMonitorService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String DLQ_KEY = "verifactu-dlq";
    private static final String BATCH_METRICS_KEY = "verifactu:batch:metrics";

    @Override
    public Long getDLQCount() {
        log.debug("Fetching DLQ count from Redis");

        try {
            Long size = redisTemplate.opsForStream().size(DLQ_KEY);
            return size != null ? size : 0L;
        } catch (Exception e) {
            log.error("Error fetching DLQ count from Redis", e);
            return 0L;
        }
    }

    @Override
    public VerifactuBatchMetrics getBatchMetrics() {
        log.debug("Fetching batch metrics from Redis");

        try {
            Map<Object, Object> metrics = redisTemplate.opsForHash().entries(BATCH_METRICS_KEY);

            return VerifactuBatchMetrics.builder()
                    .lastBatchId(getLongValue(metrics, "last_batch_id"))
                    .lastBatchTime(getStringValue(metrics, "last_batch_time"))
                    .invoicesProcessed(getLongValue(metrics, "invoices_processed"))
                    .invoicesSuccessful(getLongValue(metrics, "invoices_successful"))
                    .invoicesFailed(getLongValue(metrics, "invoices_failed"))
                    .build();
        } catch (Exception e) {
            log.error("Error fetching batch metrics from Redis", e);
            return VerifactuBatchMetrics.builder().build();  // Return empty metrics
        }
    }

    /**
     * Extract Long value from Redis map.
     */
    private Long getLongValue(Map<Object, Object> map, String key) {
        try {
            Object value = map.get(key);
            if (value instanceof String) {
                return Long.parseLong((String) value);
            } else if (value instanceof Number) {
                return ((Number) value).longValue();
            }
        } catch (Exception e) {
            log.warn("Error parsing long value for key {}: {}", key, e.getMessage());
        }
        return 0L;
    }

    /**
     * Extract String value from Redis map.
     */
    private String getStringValue(Map<Object, Object> map, String key) {
        try {
            Object value = map.get(key);
            if (value != null) {
                return value.toString();
            }
        } catch (Exception e) {
            log.warn("Error parsing string value for key {}: {}", key, e.getMessage());
        }
        return null;
    }
}
