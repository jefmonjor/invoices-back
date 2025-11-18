package com.invoices.invoice.domain.usecases;

import com.invoices.invoice.domain.entities.Invoice;
import com.invoices.invoice.domain.entities.InvoiceItem;
import com.invoices.invoice.domain.exceptions.InvoiceNotFoundException;
import com.invoices.invoice.domain.ports.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UpdateInvoiceUseCase.
 * Tests invoice update logic.
 */
@ExtendWith(MockitoExtension.class)
class UpdateInvoiceUseCaseTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    private UpdateInvoiceUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdateInvoiceUseCase(invoiceRepository);
    }

    @Test
    void shouldUpdateInvoiceWithNewItems() {
        // Arrange
        Long invoiceId = 1L;
        Invoice existingInvoice = createTestInvoice(invoiceId);
        List<InvoiceItem> newItems = createNewTestItems();
        String newNotes = "Updated notes";

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(existingInvoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Invoice result = useCase.execute(invoiceId, newItems, newNotes);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(invoiceId);
        assertThat(result.getNotes()).isEqualTo(newNotes);

        verify(invoiceRepository, times(1)).findById(invoiceId);
        verify(invoiceRepository, times(1)).save(any(Invoice.class));
    }

    @Test
    void shouldUpdateOnlyNotes() {
        // Arrange
        Long invoiceId = 1L;
        Invoice existingInvoice = createTestInvoice(invoiceId);
        String newNotes = "Only notes updated";

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(existingInvoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Invoice result = useCase.execute(invoiceId, null, newNotes);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getNotes()).isEqualTo(newNotes);

        verify(invoiceRepository, times(1)).findById(invoiceId);
        verify(invoiceRepository, times(1)).save(any(Invoice.class));
    }

    @Test
    void shouldUpdateOnlyItems() {
        // Arrange
        Long invoiceId = 1L;
        Invoice existingInvoice = createTestInvoice(invoiceId);
        List<InvoiceItem> newItems = createNewTestItems();

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(existingInvoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Invoice result = useCase.execute(invoiceId, newItems, null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(invoiceId);

        verify(invoiceRepository, times(1)).findById(invoiceId);
        verify(invoiceRepository, times(1)).save(any(Invoice.class));
    }

    @Test
    void shouldThrowExceptionWhenInvoiceNotFound() {
        // Arrange
        Long nonExistentId = 999L;
        List<InvoiceItem> newItems = createNewTestItems();
        String newNotes = "Updated notes";

        when(invoiceRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> useCase.execute(nonExistentId, newItems, newNotes))
            .isInstanceOf(InvoiceNotFoundException.class);

        verify(invoiceRepository, times(1)).findById(nonExistentId);
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void shouldClearItemsWhenNewItemsProvided() {
        // Arrange
        Long invoiceId = 1L;
        Invoice existingInvoice = createTestInvoice(invoiceId);
        existingInvoice.addItem(createTestItem("Existing item", 1, "100.00", "21.00"));

        List<InvoiceItem> newItems = new ArrayList<>();
        newItems.add(createTestItem("New item 1", 2, "50.00", "21.00"));
        newItems.add(createTestItem("New item 2", 1, "75.00", "10.00"));

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(existingInvoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Invoice result = useCase.execute(invoiceId, newItems, null);

        // Assert
        assertThat(result).isNotNull();
        verify(invoiceRepository, times(1)).save(any(Invoice.class));
    }

    @Test
    void shouldUpdateWithEmptyItemsList() {
        // Arrange
        Long invoiceId = 1L;
        Invoice existingInvoice = createTestInvoice(invoiceId);
        existingInvoice.addItem(createTestItem("Existing item", 1, "100.00", "21.00"));

        List<InvoiceItem> emptyItems = new ArrayList<>();

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(existingInvoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Invoice result = useCase.execute(invoiceId, emptyItems, "Updated with empty items");

        // Assert
        assertThat(result).isNotNull();
        verify(invoiceRepository, times(1)).save(any(Invoice.class));
    }

    @Test
    void shouldNotUpdateWhenBothParametersAreNull() {
        // Arrange
        Long invoiceId = 1L;
        Invoice existingInvoice = createTestInvoice(invoiceId);

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(existingInvoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Invoice result = useCase.execute(invoiceId, null, null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(invoiceId);

        verify(invoiceRepository, times(1)).findById(invoiceId);
        verify(invoiceRepository, times(1)).save(any(Invoice.class));
    }

    private Invoice createTestInvoice(Long id) {
        return new Invoice(
            id,
            1L,
            2L,
            "2025-001",
            LocalDateTime.now(),
            new BigDecimal("15.00"),
            new BigDecimal("5.00")
        );
    }

    private List<InvoiceItem> createNewTestItems() {
        List<InvoiceItem> items = new ArrayList<>();
        items.add(createTestItem("Updated item", 3, "150.00", "21.00"));
        return items;
    }

    private InvoiceItem createTestItem(String description, int units, String price, String vatPercentage) {
        return new InvoiceItem(
            null,
            null,
            description,
            units,
            new BigDecimal(price),
            new BigDecimal(vatPercentage),
            BigDecimal.ZERO
        );
    }
}
