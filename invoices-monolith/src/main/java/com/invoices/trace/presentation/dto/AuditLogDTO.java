package com.invoices.trace.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for AuditLog presentation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogDTO {
    private Long id;
    private String eventType;
    private Long invoiceId;
    private String invoiceNumber;
    private Long clientId;
    private String clientEmail;
    private BigDecimal total;
    private String status;
    private String eventData;
    private LocalDateTime createdAt;
}
