package com.invoices.invoice_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceDTO {
    private Integer id;
    private Integer userId;
    private Integer clientId;
    private String invoiceNumber;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime issueDate;

    private BigDecimal baseAmount;
    private BigDecimal vatPercentage;
    private BigDecimal vatAmount;
    private BigDecimal irpfPercentage;
    private BigDecimal irpfAmount;
    private BigDecimal rePercentage;
    private BigDecimal reAmount;
    private BigDecimal totalAmount;
    private String status;
    private String notes;
    private String iban;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    private List<InvoiceItemDTO> items;
}
