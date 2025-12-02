package com.invoices.invoice.infrastructure.messaging;

import com.invoices.verifactu.domain.ports.VerifactuPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Consumer for VeriFactu verification queue.
 * Implements retry logic with exponential backoff and DLQ.
 * 
 * Retry policy:
 * - Attempt 1: Immediate
 * - Attempt 2: +5s
 * - Attempt 3: +30s
 * - Attempt 4: +2min
 * - After 4 attempts: Move to DLQ
 */
@Slf4j
@Component
public class VerifactuConsumer {

    private static final int MAX_RETRIES = 4;
    private static final long[] RETRY_DELAYS_MS = { 0, 5000, 30000, 120000 }; // 0s, 5s, 30s, 2min

    private final RedisTemplate<String, Object> redisTemplate;
    private final VerifactuPort verifactuService;
    private final InvoiceStatusNotificationService notificationService;
    private final ScheduledExecutorService retryExecutor;

    @Value("${verifactu.stream.key:verifactu-queue}")
    private String streamKey;

    @Value("${verifactu.consumer.group:verifactu-processor}")
    private String consumerGroup;

    @Value("${verifactu.dlq.key:verifactu-dlq}")
    private String dlqKey;

    @Value("${verifactu.consumer.executor-pool-size:5}")
    private int executorPoolSize;

    public VerifactuConsumer(
            RedisTemplate<String, Object> redisTemplate,
            VerifactuPort verifactuService,
            InvoiceStatusNotificationService notificationService) {
        this.redisTemplate = redisTemplate;
        this.verifactuService = verifactuService;
        this.notificationService = notificationService;
        // Initialize with default, will be replaced after @Value injection
        this.retryExecutor = Executors.newScheduledThreadPool(5, r -> {
            Thread t = new Thread(r, "verifactu-retry-" + Thread.currentThread().getId());
            t.setDaemon(false);
            return t;
        });
    }

    @PreDestroy
    public void shutdown() {
        // Graceful shutdown with timeout
        if (retryExecutor != null && !retryExecutor.isShutdown()) {
            retryExecutor.shutdown();
            try {
                if (!retryExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    log.warn("VeriFactu executor did not terminate gracefully, forcing shutdown");
                    retryExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                log.error("Error waiting for VeriFactu executor shutdown", e);
                retryExecutor.shutdownNow();
            }
        }
    }

    @Scheduled(fixedDelay = 5000) // Check every 5 seconds
    public void consumeInvoices() {
        try {
            // Create consumer group if not exists
            ensureConsumerGroupExists();

            @SuppressWarnings("unchecked")
            List<MapRecord<String, Object, Object>> messages = redisTemplate.opsForStream().read(
                    org.springframework.data.redis.connection.stream.Consumer.from(consumerGroup, "consumer-1"),
                    StreamOffset.create(streamKey, ReadOffset.lastConsumed()));

            if (messages != null && !messages.isEmpty()) {
                for (MapRecord<String, Object, Object> message : messages) {
                    processMessage(message);
                }
            }
        } catch (Exception e) {
            log.error("[VeriFactu Consumer] Error consuming messages", e);
        }
    }

    private void processMessage(MapRecord<String, Object, Object> message) {
        try {
            Map<Object, Object> body = message.getValue();
            String invoiceIdStr = (String) body.get("invoiceId");
            String eventType = (String) body.getOrDefault("eventType", "INVOICE_CREATED");
            Integer retryCount = getRetryCount(body);

            if (invoiceIdStr == null) {
                log.warn("[VeriFactu Consumer] Message missing invoiceId: {}", message.getId());
                acknowledgeMessage(message);
                return;
            }

            Long invoiceId = Long.parseLong(invoiceIdStr);
            log.info("[VeriFactu Consumer] Processing invoice {} (event: {}, retry: {})",
                    invoiceId, eventType, retryCount);

            // Check retry count
            if (retryCount >= MAX_RETRIES) {
                log.error("[VeriFactu Consumer] Max retries exceeded for invoice {}, moving to DLQ", invoiceId);
                moveToDLQ(message, invoiceId);
                acknowledgeMessage(message);
                incrementMetric("verifactu:dlq:count");
                return;
            }

            try {
                // Notify frontend: Processing
                notificationService.notifyStatus(invoiceId, "processing");

                // Process with VeriFactu service
                verifactuService.sendInvoice(invoiceId);

                // Success - acknowledge message
                acknowledgeMessage(message);
                incrementMetric("verifactu:processed:success");
                log.info("[VeriFactu Consumer] Successfully processed invoice {}", invoiceId);

            } catch (Exception processingError) {
                log.error("[VeriFactu Consumer] Error processing invoice {}: {}",
                        invoiceId, processingError.getMessage());

                // Schedule retry with backoff
                handleRetry(message, invoiceId, retryCount, processingError);
                acknowledgeMessage(message);
                incrementMetric("verifactu:processed:error");
            }

        } catch (Exception e) {
            log.error("[VeriFactu Consumer] Unexpected error processing message {}: {}",
                    message.getId(), e.getMessage(), e);
            // Still acknowledge to prevent infinite loop
            acknowledgeMessage(message);
        }
    }

    private void handleRetry(MapRecord<String, Object, Object> message, Long invoiceId,
            int currentRetry, Exception error) {
        int nextRetry = currentRetry + 1;

        if (nextRetry >= MAX_RETRIES) {
            log.warn("[VeriFactu Consumer] Invoice {} will be moved to DLQ on next attempt", invoiceId);
            return;
        }

        long delayMs = RETRY_DELAYS_MS[nextRetry];
        log.info("[VeriFactu Consumer] Scheduling retry {} for invoice {} in {}ms",
                nextRetry, invoiceId, delayMs);

        // Re-publish with updated retry count after delay
        scheduleRetry(message, nextRetry, delayMs);
    }

    private void scheduleRetry(MapRecord<String, Object, Object> originalMessage,
            int retryCount, long delayMs) {
        // Use managed thread pool instead of creating new threads
        retryExecutor.schedule(() -> {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> msgValues = (Map<String, Object>) (Map<?, ?>) originalMessage.getValue();
                Map<String, Object> retryMessage = new HashMap<>(msgValues);
                retryMessage.put("retryCount", retryCount);
                retryMessage.put("eventType", "RETRY_VERIFICATION");

                redisTemplate.opsForStream().add(streamKey, retryMessage);
                log.debug("[VeriFactu Consumer] Retry {} scheduled", retryCount);

            } catch (Exception e) {
                log.error("[VeriFactu Consumer] Error scheduling retry", e);
            }
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    private void moveToDLQ(MapRecord<String, Object, Object> message, Long invoiceId) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> msgValues = (Map<String, Object>) (Map<?, ?>) message.getValue();
            Map<String, Object> dlqEntry = new HashMap<>(msgValues);
            dlqEntry.put("originalMessageId", message.getId().getValue());
            dlqEntry.put("failedAt", System.currentTimeMillis());
            dlqEntry.put("reason", "Max retries exceeded");

            redisTemplate.opsForStream().add(dlqKey, dlqEntry);
            log.info("[VeriFactu Consumer] Moved invoice {} to DLQ", invoiceId);

            // Notify frontend about permanent failure
            notificationService.notifyStatus(invoiceId, "failed");

        } catch (Exception e) {
            log.error("[VeriFactu Consumer] Error moving message to DLQ", e);
        }
    }

    private Integer getRetryCount(Map<Object, Object> body) {
        Object retryObj = body.get("retryCount");
        if (retryObj instanceof Integer) {
            return (Integer) retryObj;
        } else if (retryObj instanceof String) {
            try {
                return Integer.parseInt((String) retryObj);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    private void acknowledgeMessage(MapRecord<String, Object, Object> message) {
        try {
            redisTemplate.opsForStream().acknowledge(streamKey, consumerGroup, message.getId());
        } catch (Exception e) {
            log.error("[VeriFactu Consumer] Error acknowledging message {}", message.getId(), e);
        }
    }

    private void ensureConsumerGroupExists() {
        try {
            redisTemplate.opsForStream().createGroup(streamKey, consumerGroup);
            log.debug("[VeriFactu Consumer] Created consumer group: {}", consumerGroup);
        } catch (Exception e) {
            // Group likely already exists
        }
    }

    private void incrementMetric(String metricKey) {
        try {
            redisTemplate.opsForValue().increment(metricKey);
        } catch (Exception e) {
            log.error("[VeriFactu Consumer] Error incrementing metric {}", metricKey, e);
        }
    }

    /**
     * Get metrics for monitoring
     */
    public Map<String, Long> getMetrics() {
        Map<String, Long> metrics = new HashMap<>();
        try {
            metrics.put("processed_success", getLongMetric("verifactu:processed:success"));
            metrics.put("processed_error", getLongMetric("verifactu:processed:error"));
            metrics.put("dlq_count", getLongMetric("verifactu:dlq:count"));
        } catch (Exception e) {
            log.error("[VeriFactu Consumer] Error retrieving metrics", e);
        }
        return metrics;
    }

    private Long getLongMetric(String key) {
        String value = (String) redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value) : 0L;
    }

    @PreDestroy
    public void shutdown() {
        log.info("[VeriFactu Consumer] Shutting down retry executor service");
        retryExecutor.shutdown();
        try {
            if (!retryExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                log.warn("[VeriFactu Consumer] Executor service did not terminate within timeout, forcing shutdown");
                retryExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.error("[VeriFactu Consumer] Interrupted while waiting for executor shutdown", e);
            retryExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
