package com.invoices.verifactu.application.services;

import com.invoices.invoice.domain.entities.Company;
import com.invoices.invoice.domain.entities.Invoice;
import com.invoices.invoice.domain.ports.CompanyRepository;
import com.invoices.shared.domain.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceChainServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @InjectMocks
    private InvoiceChainService service;

    @Test
    void calculateChainedHash_ShouldGenerateHash() {
        // Arrange
        Invoice invoice = new Invoice(
                1L, 1L, 1L, "INV-001", java.time.LocalDateTime.now(), BigDecimal.ZERO, BigDecimal.ZERO);
        // Invoice total is calculated from items. If 0 items, total is 0.
        // We can't easily set total amount directly as it's calculated.
        // But the hash calculation uses getInvoiceNumber and getTotalAmount.
        // Let's assume 0 is fine for the test, or add an item if possible.

        String lastHash = "previous-hash";

        // Act
        String hash = service.calculateChainedHash(invoice, lastHash);

        // Assert
        assertNotNull(hash);
        assertFalse(hash.isEmpty());
    }

    @Test
    void validateInvoiceBeforeSending_ShouldThrowException_WhenStatusInvalid() {
        // Arrange
        Invoice invoice = new Invoice(
                1L, 1L, 1L, "INV-001", java.time.LocalDateTime.now(), BigDecimal.ZERO, BigDecimal.ZERO);
        // Status is DRAFT by default. validateInvoiceBeforeSending checks for PENDING.
        // So this should throw if we pass DRAFT.
        // Wait, the service checks: if (invoice.getStatus() != InvoiceStatus.PENDING)

        // Act & Assert
        assertThrows(BusinessException.class, () -> service.validateInvoiceBeforeSending(invoice));
    }

    @Test
    void lockTenantForUpdate_ShouldReturnCompany_WhenFound() {
        // Arrange
        Long companyId = 1L;
        Company company = new Company(
                companyId, "Test Company", "B12345678", "Address", "City", "28001", "Madrid", "600000000",
                "email@test.com", "ES0000000000000000000000");
        when(companyRepository.findByIdWithLock(companyId)).thenReturn(Optional.of(company));

        // Act
        Company result = service.lockTenantForUpdate(companyId);

        // Assert
        assertNotNull(result);
        assertEquals(companyId, result.getId());
    }

    @Test
    void updateTenantLastHash_ShouldUpdateAndSave() {
        // Arrange
        Long companyId = 1L;
        String newHash = "new-hash";
        Company company = new Company(
                companyId, "Test Company", "B12345678", "Address", "City", "28001", "Madrid", "600000000",
                "email@test.com", "ES0000000000000000000000");

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(companyRepository.save(any(Company.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        Company result = service.updateTenantLastHash(companyId, newHash);

        // Assert
        assertEquals(newHash, result.getLastHash());
        verify(companyRepository).save(any(Company.class));
    }
}
