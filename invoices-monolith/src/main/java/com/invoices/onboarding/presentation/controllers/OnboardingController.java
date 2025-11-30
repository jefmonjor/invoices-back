package com.invoices.onboarding.presentation.controllers;

import com.invoices.onboarding.application.services.OnboardingService;
import com.invoices.onboarding.presentation.dto.OnboardingProgressDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
@Tag(name = "Onboarding", description = "Endpoints for onboarding wizard")
public class OnboardingController {

    private final OnboardingService onboardingService;

    @GetMapping("/progress")
    @Operation(summary = "Get onboarding progress", description = "Get the current onboarding progress for the company")
    public ResponseEntity<OnboardingProgressDTO> getProgress() {
        return ResponseEntity.ok(onboardingService.getProgress());
    }

    @PostMapping("/step/{step}/complete")
    @Operation(summary = "Complete a step", description = "Mark a specific onboarding step as completed")
    public ResponseEntity<Void> completeStep(@PathVariable String step) {
        onboardingService.updateStep(step.toUpperCase(), true);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/skip")
    @Operation(summary = "Skip onboarding", description = "Mark the onboarding process as skipped")
    public ResponseEntity<Void> skipOnboarding() {
        onboardingService.skipOnboarding();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/demo-data/{type}")
    @Operation(summary = "Get demo data", description = "Get demo data templates for a specific type (CLIENT, INVOICE)")
    public ResponseEntity<List<String>> getDemoData(@PathVariable String type) {
        return ResponseEntity.ok(onboardingService.getDemoData(type.toUpperCase()));
    }
}
