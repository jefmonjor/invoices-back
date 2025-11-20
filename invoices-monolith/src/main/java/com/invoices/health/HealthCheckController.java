package com.invoices.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Simple health check controller for Railway deployment.
 * Provides a lightweight endpoint that always returns 200 OK,
 * independent of application dependencies (DB, Redis, MinIO).
 */
@Slf4j
@RestController
public class HealthCheckController {

    /**
     * Simple health check endpoint that always returns 200 OK.
     * Used for Railway healthcheck during deployment.
     * Does not check database, Redis, or other dependencies.
     *
     * @return HTTP 200 with simple status message
     */
    @GetMapping("/health/simple")
    public ResponseEntity<Map<String, String>> simpleHealth() {
        log.debug("Simple health check called");
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "message", "Application is running"
        ));
    }

    /**
     * Readiness probe that indicates application is ready to serve traffic.
     * This is a minimal check - for full dependency checks use /actuator/health/readiness
     *
     * @return HTTP 200 when application context is loaded
     */
    @GetMapping("/health/ready")
    public ResponseEntity<Map<String, String>> ready() {
        log.debug("Readiness check called");
        return ResponseEntity.ok(Map.of(
                "status", "READY",
                "message", "Application is ready to serve traffic"
        ));
    }
}
