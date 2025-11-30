package com.invoices.analytics.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CompanyAdminDashboardDTO extends DashboardDTO {
    private BigDecimal pendingInvoicesTotal;
    private long verifactuRejectedCount;
    private List<ClientRevenueDTO> top5Clients;
    private Map<String, BigDecimal> last30DaysRevenue; // Date -> Revenue
}
