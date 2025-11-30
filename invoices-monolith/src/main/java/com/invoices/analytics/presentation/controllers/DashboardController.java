package com.invoices.analytics.presentation.controllers;

import com.invoices.analytics.application.services.DashboardService;
import com.invoices.analytics.presentation.dto.DashboardDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Endpoints for smart dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @Operation(summary = "Get dashboard data", description = "Get dashboard data adapted to the current user's role")
    public ResponseEntity<DashboardDTO> getDashboardData() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return ResponseEntity.ok(dashboardService.getDashboardData(email));
    }
}
