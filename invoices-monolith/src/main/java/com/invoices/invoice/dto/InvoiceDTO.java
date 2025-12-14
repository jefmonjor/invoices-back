package com.invoices.invoice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for Invoice response.
 * Matches OpenAPI specification v2.0.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceDTO {
    private Long id;
    private Long companyId;
    private Long clientId;
    private CompanyDTO company;
    private ClientDTO client;
    private String invoiceNumber;
    private String settlementNumber;
    private LocalDateTime issueDate;
    private BigDecimal baseAmount;
    private BigDecimal irpfPercentage;
    private BigDecimal irpfAmount;
    private BigDecimal rePercentage;
    private BigDecimal reAmount;
    private BigDecimal totalAmount;
    private String status;
    private String verifactuStatus;
    private String documentHash;
    private String pdfServerPath;
    private String documentJson;
    private String verifactuTxId;
    private Boolean pdfIsFinal;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<InvoiceItemDTO> items;
    private String verifactuError; // Error message from VeriFactu if rejected
    private Integer verifactuRetryCount; // Number of retry attempts
}
