package com.invoices.invoice.infrastructure.messaging;

import com.invoices.invoice.infrastructure.messaging.dto.InvoiceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisEventPublisher {

    private final RedisTemplate<String, Object> redisTemplate;

    public void publish(InvoiceEvent event) {
        String channel = "tenant:" + event.getTenantId() + ":events";
        log.debug("Publishing event to channel {}: {}", channel, event);
        redisTemplate.convertAndSend(channel, event);
    }
}
