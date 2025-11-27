package com.invoices.metrics;

import com.invoices.company.infrastructure.persistence.repositories.UserCompanyRepository;
import com.invoices.invoice.domain.entities.Company;
import com.invoices.invoice.domain.ports.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Service for calculating company metrics and statistics.
 * Provides operational visibility for monitoring and analytics.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyMetricsService {

        private final UserCompanyRepository userCompanyRepository;
        private final CompanyRepository companyRepository;
        private final com.invoices.invoice.domain.ports.InvoiceRepository invoiceRepository;

        /**
         * Get comprehensive metrics for a company.
         * Currently returns user counts. Invoice/client counts pending.
         * 
         * @param companyId Company ID
         * @return CompanyMetrics with available statistics
         */
        public CompanyMetrics getCompanyMetrics(Long companyId) {
                log.debug("Calculating metrics for company: {}", companyId);

                Company company = companyRepository.findById(companyId)
                                .orElseThrow(() -> new IllegalArgumentException("Company not found: " + companyId));

                // Count users (works with existing methods)
                Long totalUsers = (long) userCompanyRepository.findByIdCompanyId(companyId).size();
                Long adminUsers = userCompanyRepository.findByIdCompanyId(companyId).stream()
                                .filter(uc -> "ADMIN".equals(uc.getRole()))
                                .count();

                long invoiceCount = invoiceRepository.findByCompanyId(companyId).size(); // Moved calculation here

                return CompanyMetrics.builder()
                                .companyId(companyId)
                                .companyName(company.getBusinessName())
                                .invoiceCount(invoiceCount) // Used the calculated value
                                .draftInvoiceCount(0L) // TODO: Add countByCompanyIdAndStatus() to InvoiceRepository
                                .finalizedInvoiceCount(0L) // TODO: Add countByCompanyIdAndStatus() to InvoiceRepository
                                .clientCount(0L) // TODO: Add countByCompanyId() to ClientRepository
                                .userCount(totalUsers)
                                .adminCount(adminUsers)
                                .generatedAt(Instant.now())
                                .build();
        }
}
