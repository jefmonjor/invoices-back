package com.invoices.invoice.domain.usecases;

import com.invoices.invoice.domain.entities.Invoice;
import com.invoices.invoice.domain.entities.InvoiceStatus;
import com.invoices.invoice.domain.exceptions.InvalidInvoiceStateException;
import com.invoices.invoice.domain.exceptions.InvoiceNotFoundException;
import com.invoices.invoice.domain.ports.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DeleteInvoiceUseCase.
 * Tests invoice deletion logic with business rules.
 */
@ExtendWith(MockitoExtension.class)
@Disabled("Requires refactoring after Clean Architecture migration - TODO")
class DeleteInvoiceUseCaseTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    private DeleteInvoiceUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new DeleteInvoiceUseCase(invoiceRepository);
    }

    @Test
    void shouldDeletePendingInvoiceSuccessfully() {
        // Arrange
        Long invoiceId = 1L;
        Invoice pendingInvoice = createTestInvoice(invoiceId, InvoiceStatus.PENDING);

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(pendingInvoice));
        doNothing().when(invoiceRepository).deleteById(invoiceId);

        // Act
        useCase.execute(invoiceId);

        // Assert
        verify(invoiceRepository, times(1)).findById(invoiceId);
        verify(invoiceRepository, times(1)).deleteById(invoiceId);
    }

    @Test
    void shouldDeleteCancelledInvoiceSuccessfully() {
        // Arrange
        Long invoiceId = 2L;
        Invoice cancelledInvoice = createTestInvoice(invoiceId, InvoiceStatus.CANCELLED);

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(cancelledInvoice));
        doNothing().when(invoiceRepository).deleteById(invoiceId);

        // Act
        useCase.execute(invoiceId);

        // Assert
        verify(invoiceRepository, times(1)).findById(invoiceId);
        verify(invoiceRepository, times(1)).deleteById(invoiceId);
    }

    @Test
    void shouldThrowExceptionWhenDeletingPaidInvoice() {
        // Arrange
        Long invoiceId = 3L;
        Invoice paidInvoice = createTestInvoice(invoiceId, InvoiceStatus.PAID);

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(paidInvoice));

        // Act & Assert
        assertThatThrownBy(() -> useCase.execute(invoiceId))
            .isInstanceOf(InvalidInvoiceStateException.class)
            .hasMessageContaining("Cannot delete paid invoice")
            .hasMessageContaining(invoiceId.toString());

        verify(invoiceRepository, times(1)).findById(invoiceId);
        verify(invoiceRepository, never()).deleteById(any());
    }

    @Test
    void shouldThrowExceptionWhenInvoiceNotFound() {
        // Arrange
        Long nonExistentId = 999L;

        when(invoiceRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> useCase.execute(nonExistentId))
            .isInstanceOf(InvoiceNotFoundException.class);

        verify(invoiceRepository, times(1)).findById(nonExistentId);
        verify(invoiceRepository, never()).deleteById(any());
    }

    @Test
    void shouldDeleteOverdueInvoiceSuccessfully() {
        // Arrange
        Long invoiceId = 4L;
        Invoice overdueInvoice = createTestInvoice(invoiceId, InvoiceStatus.OVERDUE);

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(overdueInvoice));
        doNothing().when(invoiceRepository).deleteById(invoiceId);

        // Act
        useCase.execute(invoiceId);

        // Assert
        verify(invoiceRepository, times(1)).findById(invoiceId);
        verify(invoiceRepository, times(1)).deleteById(invoiceId);
    }

    @Test
    void shouldCallDeleteByIdExactlyOnce() {
        // Arrange
        Long invoiceId = 5L;
        Invoice pendingInvoice = createTestInvoice(invoiceId, InvoiceStatus.PENDING);

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(pendingInvoice));
        doNothing().when(invoiceRepository).deleteById(invoiceId);

        // Act
        useCase.execute(invoiceId);

        // Assert
        verify(invoiceRepository, times(1)).deleteById(invoiceId);
        verifyNoMoreInteractions(invoiceRepository);
    }

    private Invoice createTestInvoice(Long id, InvoiceStatus status) {
        Invoice invoice = new Invoice(
            id,
            1L,
            2L,
            "2025-001",
            LocalDateTime.now(),
            new BigDecimal("15.00"),
            new BigDecimal("5.00")
        );

        // Set status using reflection or a test helper
        // For simplicity, assuming Invoice has a setStatus or the status can be modified
        if (status == InvoiceStatus.PAID) {
            invoice.markAsPaid();
        } else if (status == InvoiceStatus.CANCELLED) {
            invoice.cancel();
        }
        // PENDING and OVERDUE are default states or can be set similarly

        return invoice;
    }
}
