package com.invoices.invoice.infrastructure.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisVerifactuProducer {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String QUEUE_NAME = "verifactu-queue";

    public void enqueueInvoice(Long invoiceId) {
        log.info("Enqueueing invoice {} for VeriFactu verification", invoiceId);

        Map<String, String> message = Map.of(
                "invoiceId", invoiceId.toString(),
                "action", "VERIFY");

        // Using Redis Stream for reliable messaging
        redisTemplate.opsForStream().add(QUEUE_NAME, message);

        log.info("Invoice {} enqueued successfully", invoiceId);
    }
}
