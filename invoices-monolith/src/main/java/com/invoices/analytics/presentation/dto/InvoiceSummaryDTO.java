package com.invoices.analytics.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InvoiceSummaryDTO {
    private Long id;
    private String number;
    private BigDecimal amount;
    private String status;
    private String date;
}
