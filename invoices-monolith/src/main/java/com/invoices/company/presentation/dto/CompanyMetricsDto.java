package com.invoices.company.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyMetricsDto {
    private Long totalInvoices;
    private Long paidInvoices;
    private Long pendingInvoices;
    private BigDecimal totalRevenue;
    private BigDecimal pendingRevenue;
    private Long totalClients;
    private Long activeUsers;
    private LocalDate periodStart;
    private LocalDate periodEnd;
}
