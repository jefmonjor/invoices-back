package com.invoices.analytics.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Map;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PlatformAdminDashboardDTO extends DashboardDTO {
    private long activeCompaniesCount;
    private long dailyInvoicesCount;
    private double verifactuAdoptionRate;
    private List<String> inactiveCompaniesAlerts; // Names of companies inactive > 30 days
    private Map<String, Long> monthlyGrowthData; // Month -> New Companies
}
