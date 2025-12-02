package com.invoices.invoice.infrastructure.messaging;

import com.invoices.invoice.domain.ports.VerifactuVerificationPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisVerifactuProducer implements VerifactuVerificationPublisher {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String QUEUE_NAME = "verifactu-queue";

    @Override
    public void enqueueForVerification(Long invoiceId) {
        enqueueForVerification(invoiceId, "INVOICE_CREATED");
    }

    @Override
    public void enqueueForVerification(Long invoiceId, String eventType) {
        log.info("Enqueueing invoice {} for VeriFactu verification (event: {})", invoiceId, eventType);

        try {
            Map<String, String> message = Map.of(
                    "invoiceId", invoiceId.toString(),
                    "action", "VERIFY",
                    "eventType", eventType);

            // Using Redis Stream for reliable messaging
            redisTemplate.opsForStream().add(QUEUE_NAME, message);

            log.info("Invoice {} enqueued successfully", invoiceId);
        } catch (Exception e) {
            log.error("Error enqueueing invoice {} for verification", invoiceId, e);
            throw new VerificationEnqueueException("Failed to enqueue invoice for verification", e);
        }
    }
}
