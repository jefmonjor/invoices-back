package com.invoices.invoice_service.domain.usecases;

import com.invoices.invoice_service.domain.entities.Invoice;
import com.invoices.invoice_service.domain.ports.InvoiceRepository;
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
 * Tests retrieving all invoices.
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
    void shouldReturnAllInvoices() {
        // Arrange
        List<Invoice> expectedInvoices = Arrays.asList(
            createTestInvoice(1L, "2025-001"),
            createTestInvoice(2L, "2025-002"),
            createTestInvoice(3L, "2025-003")
        );

        when(invoiceRepository.findAll()).thenReturn(expectedInvoices);

        // Act
        List<Invoice> result = useCase.execute();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        assertThat(result).containsExactlyElementsOf(expectedInvoices);

        verify(invoiceRepository, times(1)).findAll();
    }

    @Test
    void shouldReturnEmptyListWhenNoInvoices() {
        // Arrange
        List<Invoice> emptyList = new ArrayList<>();

        when(invoiceRepository.findAll()).thenReturn(emptyList);

        // Act
        List<Invoice> result = useCase.execute();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(invoiceRepository, times(1)).findAll();
    }

    @Test
    void shouldReturnSingleInvoice() {
        // Arrange
        List<Invoice> singleInvoice = Arrays.asList(
            createTestInvoice(1L, "2025-001")
        );

        when(invoiceRepository.findAll()).thenReturn(singleInvoice);

        // Act
        List<Invoice> result = useCase.execute();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getInvoiceNumber()).isEqualTo("2025-001");

        verify(invoiceRepository, times(1)).findAll();
    }

    @Test
    void shouldReturnLargeListOfInvoices() {
        // Arrange
        List<Invoice> manyInvoices = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            manyInvoices.add(createTestInvoice((long) i, "2025-" + String.format("%03d", i)));
        }

        when(invoiceRepository.findAll()).thenReturn(manyInvoices);

        // Act
        List<Invoice> result = useCase.execute();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(100);

        verify(invoiceRepository, times(1)).findAll();
    }

    @Test
    void shouldCallRepositoryExactlyOnce() {
        // Arrange
        List<Invoice> invoices = Arrays.asList(
            createTestInvoice(1L, "2025-001")
        );

        when(invoiceRepository.findAll()).thenReturn(invoices);

        // Act
        useCase.execute();

        // Assert
        verify(invoiceRepository, times(1)).findAll();
        verifyNoMoreInteractions(invoiceRepository);
    }

    @Test
    void shouldReturnInvoicesWithDifferentCompanies() {
        // Arrange
        Invoice invoice1 = new Invoice(1L, 1L, 2L, "2025-001", LocalDateTime.now(), BigDecimal.ZERO, BigDecimal.ZERO);
        Invoice invoice2 = new Invoice(2L, 2L, 3L, "2025-002", LocalDateTime.now(), BigDecimal.ZERO, BigDecimal.ZERO);
        Invoice invoice3 = new Invoice(3L, 1L, 4L, "2025-003", LocalDateTime.now(), BigDecimal.ZERO, BigDecimal.ZERO);

        List<Invoice> invoices = Arrays.asList(invoice1, invoice2, invoice3);

        when(invoiceRepository.findAll()).thenReturn(invoices);

        // Act
        List<Invoice> result = useCase.execute();

        // Assert
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getCompanyId()).isEqualTo(1L);
        assertThat(result.get(1).getCompanyId()).isEqualTo(2L);
        assertThat(result.get(2).getCompanyId()).isEqualTo(1L);

        verify(invoiceRepository, times(1)).findAll();
    }

    private Invoice createTestInvoice(Long id, String invoiceNumber) {
        return new Invoice(
            id,
            1L,
            2L,
            invoiceNumber,
            LocalDateTime.now(),
            new BigDecimal("15.00"),
            new BigDecimal("5.00")
        );
    }
}
