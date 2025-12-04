package com.invoices.analytics.application.services;

import com.invoices.analytics.presentation.dto.*;
import com.invoices.invoice.domain.ports.CompanyRepository;
import com.invoices.security.utils.SecurityUtils;
import com.invoices.user.domain.entities.User;
import com.invoices.user.domain.ports.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;

    @Transactional(readOnly = true)
    public DashboardDTO getDashboardData(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (SecurityUtils.isPlatformAdmin()) {
            return getPlatformAdminDashboard();
        } else if (user.getRoles().contains("ROLE_ADMIN")) {
            return getCompanyAdminDashboard(user.getCurrentCompanyId());
        } else {
            return getCompanyUserDashboard(user.getId(), user.getCurrentCompanyId());
        }
    }

    private PlatformAdminDashboardDTO getPlatformAdminDashboard() {
        return PlatformAdminDashboardDTO.builder()
                .role("PLATFORM_ADMIN")
                .activeCompaniesCount(companyRepository.count())
                .dailyInvoicesCount(0)
                .verifactuAdoptionRate(0.0)
                .inactiveCompaniesAlerts(new ArrayList<>())
                .monthlyGrowthData(new HashMap<>())
                .build();
    }

    private CompanyAdminDashboardDTO getCompanyAdminDashboard(Long companyId) {
        // Return zeros - real analytics to be implemented later
        return CompanyAdminDashboardDTO.builder()
                .role("COMPANY_ADMIN")
                .pendingInvoicesTotal(BigDecimal.ZERO)
                .verifactuRejectedCount(0)
                .top5Clients(new ArrayList<>())
                .last30DaysRevenue(new HashMap<>())
                .build();
    }

    private CompanyUserDashboardDTO getCompanyUserDashboard(Long userId, Long companyId) {
        return CompanyUserDashboardDTO.builder()
                .role("COMPANY_USER")
                .myInvoicesThisMonth(0)
                .myLastInvoices(new ArrayList<>())
                .build();
    }
}
