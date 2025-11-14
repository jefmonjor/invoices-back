package com.invoices.gateway_service.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration for Resilience4j Circuit Breaker and Time Limiter.
 * Provides fault tolerance for downstream service calls.
 */
@Configuration
@Slf4j
public class ResilienceConfig {

    /**
     * Circuit Breaker configuration.
     *
     * Parameters:
     * - Failure rate threshold: 50% (opens circuit if 50% of requests fail)
     * - Wait duration in open state: 60 seconds
     * - Sliding window size: 10 requests (calculates failure rate from last 10 requests)
     * - Minimum number of calls: 5 (before calculating failure rate)
     * - Permitted calls in half-open: 3 (test calls when transitioning to closed)
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(60))
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .permittedNumberOfCallsInHalfOpenState(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .recordExceptions(
                        java.io.IOException.class,
                        java.util.concurrent.TimeoutException.class,
                        org.springframework.web.client.ResourceAccessException.class
                )
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);

        // Add event listeners for monitoring
        registry.circuitBreaker("userService").getEventPublisher()
                .onStateTransition(event ->
                        log.warn("Circuit Breaker [userService] state changed: {} -> {}",
                                event.getStateTransition().getFromState(),
                                event.getStateTransition().getToState()))
                .onError(event ->
                        log.error("Circuit Breaker [userService] recorded error: {}",
                                event.getThrowable().getMessage()));

        registry.circuitBreaker("invoiceService").getEventPublisher()
                .onStateTransition(event ->
                        log.warn("Circuit Breaker [invoiceService] state changed: {} -> {}",
                                event.getStateTransition().getFromState(),
                                event.getStateTransition().getToState()))
                .onError(event ->
                        log.error("Circuit Breaker [invoiceService] recorded error: {}",
                                event.getThrowable().getMessage()));

        registry.circuitBreaker("documentService").getEventPublisher()
                .onStateTransition(event ->
                        log.warn("Circuit Breaker [documentService] state changed: {} -> {}",
                                event.getStateTransition().getFromState(),
                                event.getStateTransition().getToState()))
                .onError(event ->
                        log.error("Circuit Breaker [documentService] recorded error: {}",
                                event.getThrowable().getMessage()));

        registry.circuitBreaker("traceService").getEventPublisher()
                .onStateTransition(event ->
                        log.warn("Circuit Breaker [traceService] state changed: {} -> {}",
                                event.getStateTransition().getFromState(),
                                event.getStateTransition().getToState()))
                .onError(event ->
                        log.error("Circuit Breaker [traceService] recorded error: {}",
                                event.getThrowable().getMessage()));

        log.info("Circuit Breaker Registry configured with default settings");
        return registry;
    }

    /**
     * Time Limiter configuration.
     * Sets timeout for service calls to prevent hanging requests.
     */
    @Bean
    public TimeLimiterConfig timeLimiterConfig() {
        return TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(10))
                .cancelRunningFuture(true)
                .build();
    }
}
