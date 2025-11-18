package com.invoices.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
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
 * Configuration:
 * - General endpoints: 100 requests/minute per IP
 * - Auth endpoints (/api/auth/*): 10 requests/minute per IP (stricter to prevent brute force)
 */
@Component
@Order(1)
@Slf4j
public class RateLimitingFilter implements Filter {

    private final Map<String, Bucket> generalCache = new ConcurrentHashMap<>();
    private final Map<String, Bucket> authCache = new ConcurrentHashMap<>();

    // General rate limit: 100 requests per minute
    private static final long GENERAL_CAPACITY = 100;
    private static final Duration GENERAL_REFILL_DURATION = Duration.ofMinutes(1);

    // Auth endpoints stricter limit: 10 requests per minute
    private static final long AUTH_CAPACITY = 10;
    private static final Duration AUTH_REFILL_DURATION = Duration.ofMinutes(1);

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
                String.valueOf(isAuthEndpoint ? AUTH_CAPACITY : GENERAL_CAPACITY));

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
                requestPath
            );

            httpResponse.getWriter().write(jsonResponse);
        }
    }

    /**
     * Resolves or creates a bucket for the given client IP.
     * Uses different buckets for auth and general endpoints.
     */
    private Bucket resolveBucket(String clientIp, boolean isAuthEndpoint) {
        Map<String, Bucket> cache = isAuthEndpoint ? authCache : generalCache;

        return cache.computeIfAbsent(clientIp, key -> {
            long capacity = isAuthEndpoint ? AUTH_CAPACITY : GENERAL_CAPACITY;
            Duration refillDuration = isAuthEndpoint ? AUTH_REFILL_DURATION : GENERAL_REFILL_DURATION;

            Bandwidth limit = Bandwidth.classic(
                capacity,
                Refill.intervally(capacity, refillDuration)
            );

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
        log.info("Rate limiting filter initialized with limits: General={}/min, Auth={}/min",
            GENERAL_CAPACITY, AUTH_CAPACITY);
    }

    @Override
    public void destroy() {
        generalCache.clear();
        authCache.clear();
        log.info("Rate limiting filter destroyed, caches cleared");
    }
}
