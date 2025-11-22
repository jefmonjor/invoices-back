package com.invoices.invoice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for VeriFactu verification status response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationStatusDTO {
    private String status; // NOT_SENT, PENDING, PROCESSING, ACCEPTED, REJECTED, FAILED
    private String txId;
    private String rawResponse;
    private String errorMessage;
    private Long estimatedTimeSeconds;
}
