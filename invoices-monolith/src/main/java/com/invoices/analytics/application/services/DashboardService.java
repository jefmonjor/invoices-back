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
import java.util.List;

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
        // Mock data for now, replace with real repository calls when available
        return PlatformAdminDashboardDTO.builder()
                .role("PLATFORM_ADMIN")
                .activeCompaniesCount(companyRepository.count())
                .dailyInvoicesCount(150) // Mock
                .verifactuAdoptionRate(0.45) // Mock
                .inactiveCompaniesAlerts(List.of("Empresa A", "Empresa B")) // Mock
                .monthlyGrowthData(new HashMap<>()) // Mock
                .build();
    }

    private CompanyAdminDashboardDTO getCompanyAdminDashboard(Long companyId) {
        // Mock data
        return CompanyAdminDashboardDTO.builder()
                .role("COMPANY_ADMIN")
                .pendingInvoicesTotal(new BigDecimal("12500.00")) // Mock
                .verifactuRejectedCount(3) // Mock
                .top5Clients(List.of(new ClientRevenueDTO("Cliente X", new BigDecimal("5000")))) // Mock
                .last30DaysRevenue(new HashMap<>()) // Mock
                .build();
    }

    private CompanyUserDashboardDTO getCompanyUserDashboard(Long userId, Long companyId) {
        // Mock data
        return CompanyUserDashboardDTO.builder()
                .role("COMPANY_USER")
                .myInvoicesThisMonth(12) // Mock
                .myLastInvoices(new ArrayList<>()) // Mock
                .build();
    }
}
