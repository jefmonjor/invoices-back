package com.invoices.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting filter using Bucket4j token bucket algorithm.
 * Limits requests per IP address to prevent abuse and DoS attacks.
 *
 * Configuration loaded from application.yml (rate-limit.* properties):
 * - General endpoints: Configurable requests/minute per IP (default: 100)
 * - Auth endpoints (/api/auth/*): Configurable requests/minute per IP (default:
 * 10)
 */
@Component
@Order(1)
@Slf4j
public class RateLimitingFilter implements Filter {

    private final Map<String, Bucket> generalCache = new ConcurrentHashMap<>();
    private final Map<String, Bucket> authCache = new ConcurrentHashMap<>();
    private final RateLimitProperties properties;

    public RateLimitingFilter(RateLimitProperties properties) {
        this.properties = properties;
    }

    /**
     * Configuration properties for rate limiting.
     * Loaded from application.yml under 'rate-limit' prefix.
     */
    @Configuration
    @ConfigurationProperties(prefix = "rate-limit")
    @Data
    public static class RateLimitProperties {
        private long generalCapacity = 100; // Default: 100 requests/minute
        private long generalRefillMinutes = 1; // Default: 1 minute
        private long authCapacity = 10; // Default: 10 requests/minute
        private long authRefillMinutes = 1; // Default: 1 minute
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String clientIp = getClientIp(httpRequest);
        String requestPath = httpRequest.getRequestURI();

        // Check if it's an auth endpoint
        boolean isAuthEndpoint = requestPath.startsWith("/api/auth/");

        // Get appropriate bucket
        Bucket bucket = resolveBucket(clientIp, isAuthEndpoint);

        // Try to consume 1 token
        if (bucket.tryConsume(1)) {
            // Request allowed, add rate limit headers
            long remainingTokens = bucket.getAvailableTokens();
            httpResponse.setHeader("X-Rate-Limit-Remaining", String.valueOf(remainingTokens));
            httpResponse.setHeader("X-Rate-Limit-Limit",
                    String.valueOf(isAuthEndpoint ? properties.getAuthCapacity() : properties.getGeneralCapacity()));

            chain.doFilter(request, response);
        } else {
            // Rate limit exceeded
            log.warn("Rate limit exceeded for IP: {} on path: {}", clientIp, requestPath);

            httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            httpResponse.setContentType("application/json");
            httpResponse.setHeader("X-Rate-Limit-Retry-After-Seconds", "60");

            String jsonResponse = String.format(
                    "{\"timestamp\":\"%s\",\"status\":429,\"error\":\"Too Many Requests\"," +
                            "\"message\":\"Rate limit exceeded. Please try again later.\",\"path\":\"%s\"}",
                    java.time.LocalDateTime.now().toString(),
                    requestPath);

            httpResponse.getWriter().write(jsonResponse);
        }
    }

    /**
     * Resolves or creates a bucket for the given client IP.
     * Uses different buckets for auth and general endpoints.
     * Configuration is loaded from application.yml (rate-limit.* properties).
     */
    private Bucket resolveBucket(String clientIp, boolean isAuthEndpoint) {
        Map<String, Bucket> cache = isAuthEndpoint ? authCache : generalCache;

        return cache.computeIfAbsent(clientIp, key -> {
            long capacity = isAuthEndpoint ? properties.getAuthCapacity() : properties.getGeneralCapacity();
            Duration refillDuration = isAuthEndpoint
                    ? Duration.ofMinutes(properties.getAuthRefillMinutes())
                    : Duration.ofMinutes(properties.getGeneralRefillMinutes());

            Bandwidth limit = Bandwidth.builder()
                    .capacity(capacity)
                    .refillIntervally(capacity, refillDuration)
                    .build();

            return Bucket.builder()
                    .addLimit(limit)
                    .build();
        });
    }

    /**
     * Extracts client IP address from request.
     * Considers X-Forwarded-For header for proxied requests.
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");

        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("Rate limiting filter initialized with limits: General={}/{} min, Auth={}/{} min",
                properties.getGeneralCapacity(), properties.getGeneralRefillMinutes(),
                properties.getAuthCapacity(), properties.getAuthRefillMinutes());
    }

    @Override
    public void destroy() {
        generalCache.clear();
        authCache.clear();
        log.info("Rate limiting filter destroyed, caches cleared");
    }
}
