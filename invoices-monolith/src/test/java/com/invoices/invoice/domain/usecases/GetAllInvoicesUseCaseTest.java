package com.invoices.invoice.domain.usecases;

import com.invoices.invoice.domain.entities.Invoice;
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
        List<Invoice> expectedInvoices = Arrays.asList(
                createTestInvoice(1L, "2025-001"),
                createTestInvoice(2L, "2025-002"),
                createTestInvoice(3L, "2025-003"));

        when(invoiceRepository.findByCompanyId(companyId)).thenReturn(expectedInvoices);

        // Act
        List<Invoice> result = useCase.execute(companyId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        assertThat(result).containsExactlyElementsOf(expectedInvoices);

        verify(invoiceRepository, times(1)).findByCompanyId(companyId);
    }

    @Test
    void shouldReturnEmptyListWhenNoInvoices() {
        // Arrange
        Long companyId = 1L;
        List<Invoice> emptyList = new ArrayList<>();

        when(invoiceRepository.findByCompanyId(companyId)).thenReturn(emptyList);

        // Act
        List<Invoice> result = useCase.execute(companyId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(invoiceRepository, times(1)).findByCompanyId(companyId);
    }

    @Test
    void shouldReturnSingleInvoice() {
        // Arrange
        Long companyId = 1L;
        List<Invoice> singleInvoice = Arrays.asList(
                createTestInvoice(1L, "2025-001"));

        when(invoiceRepository.findByCompanyId(companyId)).thenReturn(singleInvoice);

        // Act
        List<Invoice> result = useCase.execute(companyId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getInvoiceNumber()).isEqualTo("2025-001");

        verify(invoiceRepository, times(1)).findByCompanyId(companyId);
    }

    @Test
    void shouldReturnLargeListOfInvoices() {
        // Arrange
        Long companyId = 1L;
        List<Invoice> manyInvoices = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            manyInvoices.add(createTestInvoice((long) i, "2025-" + String.format("%03d", i)));
        }

        when(invoiceRepository.findByCompanyId(companyId)).thenReturn(manyInvoices);

        // Act
        List<Invoice> result = useCase.execute(companyId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(100);

        verify(invoiceRepository, times(1)).findByCompanyId(companyId);
    }

    @Test
    void shouldCallRepositoryExactlyOnce() {
        // Arrange
        Long companyId = 1L;
        List<Invoice> invoices = Arrays.asList(
                createTestInvoice(1L, "2025-001"));

        when(invoiceRepository.findByCompanyId(companyId)).thenReturn(invoices);

        // Act
        useCase.execute(companyId);

        // Assert
        verify(invoiceRepository, times(1)).findByCompanyId(companyId);
        verifyNoMoreInteractions(invoiceRepository);
    }

    private Invoice createTestInvoice(Long id, String invoiceNumber) {
        return new Invoice(
                id,
                1L,
                2L,
                invoiceNumber,
                LocalDateTime.now(),
                new BigDecimal("15.00"),
                new BigDecimal("5.00"));
    }
}
