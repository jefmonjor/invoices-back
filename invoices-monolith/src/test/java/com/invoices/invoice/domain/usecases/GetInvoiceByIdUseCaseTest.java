package com.invoices.invoice.domain.usecases;

import com.invoices.invoice.domain.entities.Invoice;
import com.invoices.invoice.domain.exceptions.InvoiceNotFoundException;
import com.invoices.invoice.domain.ports.ClientRepository;
import com.invoices.invoice.domain.ports.CompanyRepository;
import com.invoices.invoice.domain.ports.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GetInvoiceByIdUseCase.
 * Tests use case logic with mocked repositories.
 */
@ExtendWith(MockitoExtension.class)
class GetInvoiceByIdUseCaseTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private CompanyRepository companyRepository;

    private GetInvoiceByIdUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetInvoiceByIdUseCase(invoiceRepository, clientRepository, companyRepository);
    }

    @Test
    void shouldReturnInvoiceWhenIdIsValid() {
        // Arrange
        Long invoiceId = 1L;
        Invoice expectedInvoice = createTestInvoice(invoiceId);
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(expectedInvoice));

        // Act
        Invoice result = useCase.execute(invoiceId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(invoiceId);
        assertThat(result.getInvoiceNumber()).isEqualTo("2025-001");
        verify(invoiceRepository, times(1)).findById(invoiceId);
    }

    @Test
    void shouldThrowExceptionWhenInvoiceNotFound() {
        // Arrange
        Long nonExistentId = 999L;
        when(invoiceRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> useCase.execute(nonExistentId))
                .isInstanceOf(InvoiceNotFoundException.class)
                .hasMessageContaining("999");

        verify(invoiceRepository, times(1)).findById(nonExistentId);
    }

    @Test
    void shouldThrowExceptionWhenIdIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> useCase.execute(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invoice ID cannot be null");

        verify(invoiceRepository, never()).findById(any());
    }

    @Test
    void shouldThrowExceptionWhenIdIsZero() {
        // Act & Assert
        assertThatThrownBy(() -> useCase.execute(0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invoice ID must be positive");

        verify(invoiceRepository, never()).findById(any());
    }

    @Test
    void shouldThrowExceptionWhenIdIsNegative() {
        // Act & Assert
        assertThatThrownBy(() -> useCase.execute(-1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invoice ID must be positive");

        verify(invoiceRepository, never()).findById(any());
    }

    @Test
    void shouldThrowExceptionWhenInvoiceRepositoryIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> new GetInvoiceByIdUseCase(null, clientRepository, companyRepository))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invoice repository cannot be null");
    }

    @Test
    void shouldThrowExceptionWhenClientRepositoryIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> new GetInvoiceByIdUseCase(invoiceRepository, null, companyRepository))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Client repository cannot be null");
    }

    @Test
    void shouldThrowExceptionWhenCompanyRepositoryIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> new GetInvoiceByIdUseCase(invoiceRepository, clientRepository, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Company repository cannot be null");
    }

    private Invoice createTestInvoice(Long id) {
        return new Invoice(
                id,
                10L,
                20L,
                "2025-001",
                LocalDateTime.now(),
                new BigDecimal("15.00"),
                new BigDecimal("5.00"));
    }
}
