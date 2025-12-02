package com.invoices.company.application.services;

import com.invoices.audit.domain.ports.PlatformAdminAuditLogRepository;
import com.invoices.company.domain.ports.UserCompanyRepository;
import com.invoices.company.presentation.dto.CompanyDto;
import com.invoices.company.presentation.dto.CompanyMetricsDto;
import com.invoices.invoice.domain.entities.Company;
import com.invoices.invoice.domain.ports.CompanyRepository;
import com.invoices.invoice.domain.ports.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlatformAdminServiceTest {

    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private UserCompanyRepository userCompanyRepository;
    @Mock
    private PlatformAdminAuditLogRepository auditLogRepository;

    @InjectMocks
    private PlatformAdminService platformAdminService;

    @BeforeEach
    void setUp() {
        // Mock Security Context with PLATFORM_ADMIN
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin@platform.com", "password",
                        List.of(new SimpleGrantedAuthority("ROLE_PLATFORM_ADMIN"))));
    }

    @Test
    void shouldGetAllCompanies() {
        // Given
        Company company = new Company(1L, "Test Corp", "123", "Addr", "City", "12345", "Prov", "Country", "Phone",
                "email", "iban", null, null);
        when(companyRepository.findAll()).thenReturn(List.of(company));

        // When
        List<CompanyDto> result = platformAdminService.getAllCompanies();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(companyRepository).findAll();
    }

    @Test
    void shouldThrowAccessDeniedIfNotPlatformAdmin() {
        // Given
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "user@company.com", "password",
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        // When/Then
        assertThrows(AccessDeniedException.class, () -> platformAdminService.getAllCompanies());
    }

    @Test
    void shouldDeleteCompanyForcefully() {
        // Given
        Long companyId = 1L;
        when(invoiceRepository.countByCompanyId(companyId)).thenReturn(5L);

        // When
        platformAdminService.deleteCompany(companyId, true);

        // Then
        verify(invoiceRepository).deleteByCompanyId(companyId);
        verify(userCompanyRepository).deleteByIdCompanyId(companyId);
        verify(companyRepository).deleteById(companyId);
        verify(auditLogRepository).save(any());
    }

    @Test
    void shouldFailToDeleteCompanyWithInvoicesWithoutForce() {
        // Given
        Long companyId = 1L;
        when(invoiceRepository.countByCompanyId(companyId)).thenReturn(5L);

        // When/Then
        assertThrows(IllegalStateException.class, () -> platformAdminService.deleteCompany(companyId, false));
        verify(companyRepository, never()).deleteById(any());
    }

    @Test
    void shouldGetCompanyMetrics() {
        // Given
        Long companyId = 1L;
        when(invoiceRepository.countByCompanyId(companyId)).thenReturn(10L);
        when(userCompanyRepository.countByIdCompanyId(companyId)).thenReturn(3L);

        // When
        CompanyMetricsDto metrics = platformAdminService.getCompanyMetrics(companyId);

        // Then
        assertEquals(10L, metrics.getTotalInvoices());
        assertEquals(3L, metrics.getActiveUsers());
        assertEquals(java.math.BigDecimal.ZERO, metrics.getTotalRevenue());
    }
}
