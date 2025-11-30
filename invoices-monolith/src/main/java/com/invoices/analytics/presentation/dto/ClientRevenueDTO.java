package com.invoices.analytics.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClientRevenueDTO {
    private String clientName;
    private BigDecimal totalRevenue;
}
