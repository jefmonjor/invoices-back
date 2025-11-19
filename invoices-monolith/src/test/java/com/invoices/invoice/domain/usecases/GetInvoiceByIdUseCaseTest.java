package com.invoices.invoice.domain.usecases;

import com.invoices.invoice.domain.entities.Invoice;
import com.invoices.invoice.domain.exceptions.InvoiceNotFoundException;
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
 * Tests use case logic with mocked repository.
 */
@ExtendWith(MockitoExtension.class)
class GetInvoiceByIdUseCaseTest {

    @Mock
    private InvoiceRepository repository;

    private GetInvoiceByIdUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetInvoiceByIdUseCase(repository);
    }

    @Test
    void shouldReturnInvoiceWhenIdIsValid() {
        // Arrange
        Long invoiceId = 1L;
        Invoice expectedInvoice = createTestInvoice(invoiceId);
        when(repository.findById(invoiceId)).thenReturn(Optional.of(expectedInvoice));

        // Act
        Invoice result = useCase.execute(invoiceId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(invoiceId);
        assertThat(result.getInvoiceNumber()).isEqualTo("2025-001");
        verify(repository, times(1)).findById(invoiceId);
    }

    @Test
    void shouldThrowExceptionWhenInvoiceNotFound() {
        // Arrange
        Long nonExistentId = 999L;
        when(repository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> useCase.execute(nonExistentId))
            .isInstanceOf(InvoiceNotFoundException.class)
            .hasMessageContaining("999");

        verify(repository, times(1)).findById(nonExistentId);
    }

    @Test
    void shouldThrowExceptionWhenIdIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> useCase.execute(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invoice ID cannot be null");

        verify(repository, never()).findById(any());
    }

    @Test
    void shouldThrowExceptionWhenIdIsZero() {
        // Act & Assert
        assertThatThrownBy(() -> useCase.execute(0L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invoice ID must be positive");

        verify(repository, never()).findById(any());
    }

    @Test
    void shouldThrowExceptionWhenIdIsNegative() {
        // Act & Assert
        assertThatThrownBy(() -> useCase.execute(-1L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invoice ID must be positive");

        verify(repository, never()).findById(any());
    }

    @Test
    void shouldThrowExceptionWhenRepositoryIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> new GetInvoiceByIdUseCase(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Repository cannot be null");
    }

    private Invoice createTestInvoice(Long id) {
        return new Invoice(
            id,
            10L,
            20L,
            "2025-001",
            LocalDateTime.now(),
            new BigDecimal("15.00"),
            new BigDecimal("5.00")
        );
    }
}
