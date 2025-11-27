package com.invoices.metrics;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for company metrics and monitoring.
 * Provides operational visibility into company usage and statistics.
 */
@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
@Tag(name = "Metrics", description = "Company metrics and monitoring endpoints")
public class CompanyMetricsController {

    private final CompanyMetricsService metricsService;

    @GetMapping("/company/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get company metrics", description = "**ADMIN only.** Returns comprehensive metrics for a company including invoice counts, client counts, and user statistics.", responses = {
            @ApiResponse(responseCode = "200", description = "Metrics retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "User is not ADMIN"),
            @ApiResponse(responseCode = "404", description = "Company not found")
    })
    @Parameter(name = "id", description = "Company ID", required = true)
    public ResponseEntity<CompanyMetrics> getCompanyMetrics(@PathVariable Long id) {
        CompanyMetrics metrics = metricsService.getCompanyMetrics(id);
        return ResponseEntity.ok(metrics);
    }
}
