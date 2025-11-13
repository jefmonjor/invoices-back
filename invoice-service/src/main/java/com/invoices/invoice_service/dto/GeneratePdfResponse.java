package com.invoices.invoice_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratePdfResponse {
    private Long invoiceId;
    private Long documentId;
    private String downloadUrl;
    private LocalDateTime generatedAt;
}
