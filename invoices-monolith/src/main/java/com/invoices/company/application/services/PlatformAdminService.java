package com.invoices.company.application.services;

import com.invoices.audit.domain.entities.PlatformAdminAuditLog;
import com.invoices.audit.infrastructure.persistence.repositories.PlatformAdminAuditLogRepository;
import com.invoices.company.infrastructure.persistence.repositories.UserCompanyRepository;
import com.invoices.company.presentation.dto.CompanyDto;
import com.invoices.company.presentation.dto.CompanyMetricsDto;
import com.invoices.invoice.domain.ports.CompanyRepository;
import com.invoices.invoice.domain.ports.InvoiceRepository;
import com.invoices.security.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for Platform Administrator operations.
 * Handles company management at a platform level.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlatformAdminService {

    private final CompanyRepository companyRepository;
    private final InvoiceRepository invoiceRepository;
    private final UserCompanyRepository userCompanyRepository;
    private final PlatformAdminAuditLogRepository auditLogRepository;

    /**
     * Get all companies in the platform.
     *
     * @return list of all companies
     */
    @Transactional(readOnly = true)
    public List<CompanyDto> getAllCompanies() {
        SecurityUtils.requirePlatformAdmin();

        return companyRepository.findAll().stream()
                .map(CompanyDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Delete a company.
     *
     * @param companyId the company ID
     * @param force     if true, delete even if it has invoices
     */
    @Transactional
    public void deleteCompany(Long companyId, boolean force) {
        SecurityUtils.requirePlatformAdmin();

        long invoiceCount = invoiceRepository.countByCompanyId(companyId);

        if (!force && invoiceCount > 0) {
            throw new IllegalStateException(
                    String.format("Company has %d invoices. Use force=true to delete anyway", invoiceCount));
        }

        log.info("Platform Admin deleting company ID: {}, force: {}", companyId, force);

        // Delete all invoices if force is true (or if count is 0, this does nothing)
        if (force && invoiceCount > 0) {
            invoiceRepository.deleteByCompanyId(companyId);
        }

        // Delete user associations
        userCompanyRepository.deleteByIdCompanyId(companyId);

        // Delete company
        companyRepository.deleteById(companyId);

        // Audit log
        String adminEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        auditLogRepository.save(new PlatformAdminAuditLog(
                adminEmail,
                "DELETE_COMPANY",
                "COMPANY",
                companyId.toString(),
                "Force: " + force + ", Invoices: " + invoiceCount));
    }

    /**
     * Get company metrics (without accessing invoice details).
     *
     * @param companyId the company ID
     * @return company metrics
     */
    @Transactional(readOnly = true)
    public CompanyMetricsDto getCompanyMetrics(Long companyId) {
        SecurityUtils.requirePlatformAdmin();

        long invoiceCount = invoiceRepository.countByCompanyId(companyId);
        long userCount = userCompanyRepository.countByIdCompanyId(companyId);

        // Note: We cannot calculate revenue easily without accessing invoice details,
        // which might be restricted. For now, we return 0 or implement a specific
        // repository method that aggregates revenue at DB level without loading
        // entities.
        // For safety and strict compliance with "no invoice access", we return 0
        // revenue.

        CompanyMetricsDto metrics = new CompanyMetricsDto();
        metrics.setTotalInvoices(invoiceCount);
        metrics.setActiveUsers(userCount);
        metrics.setTotalRevenue(java.math.BigDecimal.ZERO);
        metrics.setPaidInvoices(0L); // Detailed status not available
        metrics.setPendingInvoices(0L); // Detailed status not available
        // Overdue not in DTO, skipping or adding if needed. DTO has pendingRevenue,
        // setting to 0.
        metrics.setPendingRevenue(java.math.BigDecimal.ZERO);

        return metrics;
    }
}
