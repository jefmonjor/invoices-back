package com.invoices.invoice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for VERI*FACTU verification status response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationStatusDTO {

    /**
     * Invoice ID
     */
    private Long invoiceId;

    /**
     * Invoice number (e.g., "2025-058")
     */
    private String invoiceNumber;

    /**
     * VERI*FACTU status: PENDING, PROCESSING, ACCEPTED, REJECTED, FAILED
     */
    private String verifactuStatus;

    /**
     * VERI*FACTU transaction ID (e.g., "VF-2025-12345" or "MOCK-2025-ABC123")
     */
    private String verifactuTxId;

    /**
     * SHA-256 hash of the canonical JSON (64 hex chars)
     */
    private String documentHash;

    /**
     * QR code payload (URL or data for QR embedding in PDF)
     */
    private String qrPayload;

    /**
     * Whether the PDF is the final verified version
     */
    private Boolean pdfIsFinal;

    /**
     * Invoice creation timestamp
     */
    private LocalDateTime createdAt;

    /**
     * Last update timestamp
     */
    private LocalDateTime updatedAt;

    /**
     * Raw JSON response from VERI*FACTU (for debugging/audit)
     */
    private String rawResponse;

    /**
     * Error message (if status is REJECTED or FAILED)
     */
    private String errorMessage;

    /**
     * Estimated time remaining for processing (seconds)
     */
    private Long estimatedTimeSeconds;
}
