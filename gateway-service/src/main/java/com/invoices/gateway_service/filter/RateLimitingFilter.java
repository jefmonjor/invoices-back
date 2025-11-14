package com.invoices.gateway_service.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting filter using Token Bucket algorithm via Bucket4j.
 * Limits requests per IP address to prevent abuse.
 *
 * Default: 100 requests per minute per IP
 */
@Component
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int CAPACITY = 100; // Maximum tokens
    private static final int REFILL_TOKENS = 100; // Tokens to refill
    private static final Duration REFILL_DURATION = Duration.ofMinutes(1); // Refill every minute

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String clientIp = getClientIP(request);
        Bucket bucket = resolveBucket(clientIp);

        if (bucket.tryConsume(1)) {
            // Request allowed
            log.debug("Request from IP {} allowed. Remaining tokens: {}",
                    clientIp, bucket.getAvailableTokens());
            filterChain.doFilter(request, response);
        } else {
            // Rate limit exceeded
            log.warn("Rate limit exceeded for IP: {}", clientIp);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\": \"Too many requests\", " +
                    "\"message\": \"Rate limit exceeded. Please try again later.\", " +
                    "\"limit\": \"" + CAPACITY + " requests per minute\"}"
            );
        }
    }

    /**
     * Resolve or create a bucket for the given client IP
     */
    private Bucket resolveBucket(String clientIp) {
        return cache.computeIfAbsent(clientIp, key -> createNewBucket());
    }

    /**
     * Create a new bucket with token bucket algorithm
     */
    private Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.classic(
                CAPACITY,
                Refill.intervally(REFILL_TOKENS, REFILL_DURATION)
        );
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Extract client IP from request, considering proxies
     */
    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isEmpty()) {
            return xfHeader.split(",")[0].trim();
        }

        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }

        return request.getRemoteAddr();
    }

    /**
     * Don't rate limit health checks and actuator endpoints
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator/") ||
               path.equals("/actuator") ||
               path.equals("/health");
    }
}
