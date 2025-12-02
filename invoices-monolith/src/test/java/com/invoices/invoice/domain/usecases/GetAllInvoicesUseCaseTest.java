package com.invoices.invoice.domain.usecases;

import com.invoices.invoice.domain.models.InvoiceSummary;
import com.invoices.invoice.domain.ports.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GetAllInvoicesUseCase.
 * Tests retrieving all invoices for a specific company.
 */
@ExtendWith(MockitoExtension.class)
class GetAllInvoicesUseCaseTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    private GetAllInvoicesUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetAllInvoicesUseCase(invoiceRepository);
    }

    @Test
    void shouldReturnAllInvoicesForCompany() {
        // Arrange
        Long companyId = 1L;
        List<InvoiceSummary> expectedInvoices = Arrays.asList(
                createTestInvoiceSummary(1L, "2025-001"),
                createTestInvoiceSummary(2L, "2025-002"),
                createTestInvoiceSummary(3L, "2025-003"));

        when(invoiceRepository.findSummariesByCompanyId(companyId, 0, 50)).thenReturn(expectedInvoices);

        // Act
        List<InvoiceSummary> result = useCase.execute(companyId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        assertThat(result).containsExactlyElementsOf(expectedInvoices);

        verify(invoiceRepository, times(1)).findSummariesByCompanyId(companyId, 0, 50);
    }

    @Test
    void shouldReturnEmptyListWhenNoInvoices() {
        // Arrange
        Long companyId = 1L;
        List<InvoiceSummary> emptyList = new ArrayList<>();

        when(invoiceRepository.findSummariesByCompanyId(companyId, 0, 50)).thenReturn(emptyList);

        // Act
        List<InvoiceSummary> result = useCase.execute(companyId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(invoiceRepository, times(1)).findSummariesByCompanyId(companyId, 0, 50);
    }

    @Test
    void shouldReturnSingleInvoice() {
        // Arrange
        Long companyId = 1L;
        List<InvoiceSummary> singleInvoice = Arrays.asList(
                createTestInvoiceSummary(1L, "2025-001"));

        when(invoiceRepository.findSummariesByCompanyId(companyId, 0, 50)).thenReturn(singleInvoice);

        // Act
        List<InvoiceSummary> result = useCase.execute(companyId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).invoiceNumber()).isEqualTo("2025-001");

        verify(invoiceRepository, times(1)).findSummariesByCompanyId(companyId, 0, 50);
    }

    @Test
    void shouldReturnLargeListOfInvoices() {
        // Arrange
        Long companyId = 1L;
        List<InvoiceSummary> manyInvoices = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            manyInvoices.add(createTestInvoiceSummary((long) i, "2025-" + String.format("%03d", i)));
        }

        when(invoiceRepository.findSummariesByCompanyId(companyId, 0, 50)).thenReturn(manyInvoices);

        // Act
        List<InvoiceSummary> result = useCase.execute(companyId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(100);

        verify(invoiceRepository, times(1)).findSummariesByCompanyId(companyId, 0, 50);
    }

    @Test
    void shouldCallRepositoryExactlyOnce() {
        // Arrange
        Long companyId = 1L;
        List<InvoiceSummary> invoices = Arrays.asList(
                createTestInvoiceSummary(1L, "2025-001"));

        when(invoiceRepository.findSummariesByCompanyId(companyId, 0, 50)).thenReturn(invoices);

        // Act
        useCase.execute(companyId);

        // Assert
        verify(invoiceRepository, times(1)).findSummariesByCompanyId(companyId, 0, 50);
        verifyNoMoreInteractions(invoiceRepository);
    }

    private InvoiceSummary createTestInvoiceSummary(Long id, String invoiceNumber) {
        return new InvoiceSummary(
                id,
                invoiceNumber,
                LocalDateTime.now(),
                new BigDecimal("20.00"),
                "DRAFT",
                2L,
                1L);
    }
}
