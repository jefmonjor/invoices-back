package com.invoices.analytics.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CompanyUserDashboardDTO extends DashboardDTO {
    private long myInvoicesThisMonth;
    private List<InvoiceSummaryDTO> myLastInvoices;
}
