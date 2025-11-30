package com.invoices.company.presentation.controllers;

import com.invoices.company.application.services.PlatformAdminService;
import com.invoices.company.presentation.dto.CompanyDto;
import com.invoices.company.presentation.dto.CompanyMetricsDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/platform/companies")
@RequiredArgsConstructor
@Tag(name = "Platform Administration", description = "Endpoints for Platform Administrators")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class PlatformAdminController {

    private final PlatformAdminService platformAdminService;

    @GetMapping
    @Operation(summary = "List all companies", description = "Get a list of all companies in the platform")
    public ResponseEntity<List<CompanyDto>> getAllCompanies() {
        return ResponseEntity.ok(platformAdminService.getAllCompanies());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a company", description = "Delete a company. Use force=true to delete even if it has invoices.")
    public ResponseEntity<Void> deleteCompany(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean force) {
        platformAdminService.deleteCompany(id, force);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/metrics")
    @Operation(summary = "Get company metrics", description = "Get basic metrics for a company (without accessing invoice details)")
    public ResponseEntity<CompanyMetricsDto> getCompanyMetrics(@PathVariable Long id) {
        return ResponseEntity.ok(platformAdminService.getCompanyMetrics(id));
    }

    @GetMapping("/users")
    @Operation(summary = "List all users", description = "Get a list of all users in the platform")
    public ResponseEntity<List<com.invoices.user.presentation.dto.UserDTO>> getAllUsers() {
        return ResponseEntity.ok(platformAdminService.getAllUsers());
    }

    @DeleteMapping("/users/{id}")
    @Operation(summary = "Delete a user", description = "Delete (deactivate) a user from the platform")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        platformAdminService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
