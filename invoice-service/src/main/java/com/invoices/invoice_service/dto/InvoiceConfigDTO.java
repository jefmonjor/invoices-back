package com.invoices.invoice_service.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceConfigDTO {
    private String invoiceNumber;
    private Integer userId;
    private Integer clientId;
    private BigDecimal baseAmount;
    private BigDecimal irpfPercentage;
    private BigDecimal rePercentage;
    private BigDecimal totalAmount;
    private String color;
    private String textStyle;
    private String notes;
    private String iban;
    private List<InvoiceItemDTO> items;
}
