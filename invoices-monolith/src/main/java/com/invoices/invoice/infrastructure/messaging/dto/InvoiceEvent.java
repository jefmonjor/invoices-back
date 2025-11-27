package com.invoices.invoice.infrastructure.messaging.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceEvent implements Serializable {
    private String eventType; // e.g., "INVOICE_UPDATED", "VERIFACTU_SUBMITTED"
    private Long tenantId;
    private Long companyId;
    private Long invoiceId;
    private String invoiceNumber;
    private String status;
    private String verifactuStatus;
    private String txId;
    private String errorMessage;
    private LocalDateTime timestamp;
}
