package com.invoices.invoice.presentation.controllers;

import com.invoices.invoice.domain.usecases.GeneratePdfWithVerificationUseCase;
import com.invoices.shared.domain.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * REST controller for Invoice PDF generation with VeriFactu verification.
 *
 * Delegates all business logic to domain use cases.
 * Responsible only for:
 * - Extracting HTTP request parameters
 * - Calling appropriate use case
 * - Returning HTTP responses
 *
 * No business logic should be in this controller.
 */
@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Invoices", description = "Endpoints for invoice management")
public class InvoicePdfController {

    // Inject use case instead of repositories
    private final GeneratePdfWithVerificationUseCase generatePdfUseCase;

    /**
     * POST /api/invoices/{id}/generate-pdf - Generate PDF and queue verification
     *
     * Generates draft PDF URL and enqueues the invoice for VeriFactu verification.
     * Returns 202 Accepted with Location header to the draft PDF.
     *
     * Business logic:
     * 1. Validates invoice exists
     * 2. Generates draft PDF URL
     * 3. Enqueues for VeriFactu verification
     *
     * @param id Invoice ID
     * @return 202 Accepted with Location header and response body
     */
    @PostMapping("/{id}/generate-pdf")
    @Operation(summary = "Generate PDF", description = "Generate invoice PDF and queue for VeriFactu verification")
    public ResponseEntity<GeneratePdfWithVerificationUseCase.GeneratePdfResponse> generatePdfWithVerification(
            @PathVariable Long id) {
        log.info("POST /api/invoices/{}/generate-pdf - Generating PDF with verification", id);

        try {
            // Execute use case
            GeneratePdfWithVerificationUseCase.GeneratePdfResponse response = generatePdfUseCase.execute(id);

            log.info("PDF generation initiated for invoice {}, verification enqueued: {}",
                    id, response.isVerificationEnqueued());

            // Return 202 Accepted with Location header
            return ResponseEntity
                    .accepted()
                    .location(URI.create(response.getDraftPdfUrl()))
                    .body(response);

        } catch (ResourceNotFoundException e) {
            log.warn("Invoice not found: {}", id);
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            log.error("Error generating PDF for invoice {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
