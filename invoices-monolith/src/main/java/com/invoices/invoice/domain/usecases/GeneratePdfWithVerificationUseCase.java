package com.invoices.invoice.domain.usecases;

import com.invoices.invoice.domain.entities.Invoice;
import com.invoices.invoice.domain.ports.InvoiceRepository;
import com.invoices.invoice.domain.ports.VerifactuVerificationPublisher;
import com.invoices.shared.domain.exception.ResourceNotFoundException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * UseCase for generating PDF and queueing VeriFactu verification.
 *
 * Encapsulates business logic for:
 * - Invoice validation
 * - PDF URL generation
 * - VeriFactu verification enqueuing
 */
@Slf4j
public class GeneratePdfWithVerificationUseCase {

    private final InvoiceRepository invoiceRepository;
    private final VerifactuVerificationPublisher verificationPublisher;

    public GeneratePdfWithVerificationUseCase(
            InvoiceRepository invoiceRepository,
            VerifactuVerificationPublisher verificationPublisher) {
        this.invoiceRepository = invoiceRepository;
        this.verificationPublisher = verificationPublisher;
    }

    /**
     * Generate PDF for invoice and enqueue for VeriFactu verification.
     *
     * Business logic:
     * 1. Validate invoice exists
     * 2. Generate draft PDF URL
     * 3. Enqueue for VeriFactu verification
     * 4. Return response with draft PDF URL and status
     *
     * Note: PDF file is NOT generated in this method.
     * PDF generation happens asynchronously on the frontend.
     * This method only generates the URL and initiates verification.
     *
     * @param invoiceId ID of the invoice
     * @return GeneratePdfResponse with draft URL and status
     * @throws ResourceNotFoundException if invoice doesn't exist
     */
    public GeneratePdfResponse execute(Long invoiceId) {
        log.debug("Generating PDF and queueing verification for invoice {}", invoiceId);

        // 1. Validate invoice exists
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + invoiceId));

        // 2. Generate draft PDF URL
        String draftPdfUrl = String.format("/api/invoices/%d/pdf", invoiceId);

        // 3. Enqueue for VeriFactu verification
        try {
            verificationPublisher.enqueueForVerification(
                    invoiceId,
                    "INVOICE_CREATED");

            log.info("Invoice {} queued for verification", invoiceId);

            // Successfully enqueued
            return GeneratePdfResponse.builder()
                    .message("PDF generation initiated")
                    .draftPdfUrl(draftPdfUrl)
                    .invoiceId(invoiceId)
                    .verifactuStatus(invoice.getVerifactuStatus())
                    .pdfIsFinal(false)
                    .verificationEnqueued(true)
                    .build();

        } catch (VerifactuVerificationPublisher.VerificationEnqueueException e) {
            log.warn("Failed to enqueue verification for invoice {}: {}", invoiceId, e.getMessage());

            // Enqueue failed - still return draft URL but indicate failure
            return GeneratePdfResponse.builder()
                    .message("PDF generation initiated but verification could not be enqueued")
                    .draftPdfUrl(draftPdfUrl)
                    .invoiceId(invoiceId)
                    .verifactuStatus(invoice.getVerifactuStatus())
                    .pdfIsFinal(false)
                    .verificationEnqueued(false)
                    .error(e.getMessage())
                    .build();
        }
    }

    /**
     * Response DTO for PDF generation request.
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GeneratePdfResponse {
        private String message;                 // Status message
        private String draftPdfUrl;             // URL for draft PDF
        private Long invoiceId;                 // Invoice ID
        private String verifactuStatus;         // Current VeriFactu status
        private boolean pdfIsFinal;             // Is PDF final (always false for now)
        private boolean verificationEnqueued;   // Was verification enqueued successfully
        private String error;                   // Error message if failed (null if success)
    }
}
