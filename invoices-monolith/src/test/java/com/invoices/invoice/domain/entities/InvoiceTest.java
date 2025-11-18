package com.invoices.invoice.domain.entities;

import com.invoices.invoice.domain.exceptions.InvalidInvoiceNumberFormatException;
import com.invoices.invoice.domain.exceptions.InvalidInvoiceStateException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for Invoice entity.
 * Comprehensive coverage of business logic.
 */
class InvoiceTest {

    @Test
    void shouldCreateValidInvoice() {
        // Arrange & Act
        Invoice invoice = new Invoice(
            1L,
            10L,
            20L,
            "2025-001",
            LocalDateTime.now(),
            new BigDecimal("15.00"),
            new BigDecimal("5.00")
        );

        // Assert
        assertThat(invoice.getId()).isEqualTo(1L);
        assertThat(invoice.getUserId()).isEqualTo(10L);
        assertThat(invoice.getClientId()).isEqualTo(20L);
        assertThat(invoice.getInvoiceNumber()).isEqualTo("2025-001");
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.DRAFT);
    }

    @Test
    void shouldAddItemToInvoice() {
        // Arrange
        Invoice invoice = createValidInvoice();
        InvoiceItem item = createValidItem();

        // Act
        invoice.addItem(item);

        // Assert
        assertThat(invoice.getItems()).hasSize(1);
        assertThat(invoice.getItems().get(0)).isEqualTo(item);
    }

    @Test
    void shouldCalculateBaseAmountCorrectly() {
        // Arrange
        Invoice invoice = createValidInvoice();
        invoice.addItem(createItemWithPrice(new BigDecimal("100.00")));
        invoice.addItem(createItemWithPrice(new BigDecimal("200.00")));

        // Act
        BigDecimal baseAmount = invoice.calculateBaseAmount();

        // Assert
        assertThat(baseAmount).isEqualByComparingTo("300.00");
    }

    @Test
    void shouldCalculateIrpfAmountCorrectly() {
        // Arrange
        Invoice invoice = createValidInvoice();
        invoice.addItem(createItemWithPrice(new BigDecimal("100.00")));

        // Act
        BigDecimal irpfAmount = invoice.calculateIrpfAmount();

        // Assert
        assertThat(irpfAmount).isEqualByComparingTo("15.00");
    }

    @Test
    void shouldCalculateTotalAmountCorrectly() {
        // Arrange
        Invoice invoice = createValidInvoice();
        invoice.addItem(createItemWithPrice(new BigDecimal("100.00")));

        // Act
        BigDecimal totalAmount = invoice.calculateTotalAmount();

        // Assert
        assertThat(totalAmount).isPositive();
    }

    @Test
    void shouldMarkInvoiceAsPending() {
        // Arrange
        Invoice invoice = createValidInvoice();
        invoice.addItem(createValidItem());

        // Act
        invoice.markAsPending();

        // Assert
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PENDING);
    }

    @Test
    void shouldThrowExceptionWhenMarkingEmptyInvoiceAsPending() {
        // Arrange
        Invoice invoice = createValidInvoice();

        // Act & Assert
        assertThatThrownBy(invoice::markAsPending)
            .isInstanceOf(InvalidInvoiceStateException.class)
            .hasMessageContaining("without items");
    }

    @Test
    void shouldMarkInvoiceAsPaid() {
        // Arrange
        Invoice invoice = createValidInvoice();
        invoice.addItem(createValidItem());
        invoice.markAsPending();

        // Act
        invoice.markAsPaid();

        // Assert
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
    }

    @Test
    void shouldThrowExceptionWhenMarkingNonPendingInvoiceAsPaid() {
        // Arrange
        Invoice invoice = createValidInvoice();

        // Act & Assert
        assertThatThrownBy(invoice::markAsPaid)
            .isInstanceOf(InvalidInvoiceStateException.class)
            .hasMessageContaining("pending invoices");
    }

    @Test
    void shouldCancelInvoice() {
        // Arrange
        Invoice invoice = createValidInvoice();

        // Act
        invoice.cancel();

        // Assert
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.CANCELLED);
    }

    @Test
    void shouldThrowExceptionWhenCancellingPaidInvoice() {
        // Arrange
        Invoice invoice = createValidInvoice();
        invoice.addItem(createValidItem());
        invoice.markAsPending();
        invoice.markAsPaid();

        // Act & Assert
        assertThatThrownBy(invoice::cancel)
            .isInstanceOf(InvalidInvoiceStateException.class)
            .hasMessageContaining("Cannot cancel paid invoices");
    }

    @Test
    void shouldThrowExceptionWhenAddingItemToFinalizedInvoice() {
        // Arrange
        Invoice invoice = createValidInvoice();
        invoice.addItem(createValidItem());
        invoice.markAsPending();

        // Act & Assert
        assertThatThrownBy(() -> invoice.addItem(createValidItem()))
            .isInstanceOf(InvalidInvoiceStateException.class)
            .hasMessageContaining("Cannot modify invoice");
    }

    @Test
    void shouldSetNotes() {
        // Arrange
        Invoice invoice = createValidInvoice();
        String notes = "Important notes";

        // Act
        invoice.setNotes(notes);

        // Assert
        assertThat(invoice.getNotes()).isEqualTo(notes);
    }

    @Test
    void shouldThrowExceptionForInvalidInvoiceNumberFormat() {
        // Act & Assert
        assertThatThrownBy(() ->
            new Invoice(1L, 10L, 20L, "INVALID", LocalDateTime.now(),
                BigDecimal.ZERO, BigDecimal.ZERO)
        )
        .isInstanceOf(InvalidInvoiceNumberFormatException.class);
    }

    @Test
    void shouldThrowExceptionForNullInvoiceNumber() {
        // Act & Assert
        assertThatThrownBy(() ->
            new Invoice(1L, 10L, 20L, null, LocalDateTime.now(),
                BigDecimal.ZERO, BigDecimal.ZERO)
        )
        .isInstanceOf(InvalidInvoiceNumberFormatException.class);
    }

    @Test
    void shouldThrowExceptionForNegativeIrpfPercentage() {
        // Act & Assert
        assertThatThrownBy(() ->
            new Invoice(1L, 10L, 20L, "2025-001", LocalDateTime.now(),
                new BigDecimal("-5"), BigDecimal.ZERO)
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("IRPF percentage cannot be negative");
    }

    @Test
    void shouldThrowExceptionForNegativeRePercentage() {
        // Act & Assert
        assertThatThrownBy(() ->
            new Invoice(1L, 10L, 20L, "2025-001", LocalDateTime.now(),
                BigDecimal.ZERO, new BigDecimal("-3"))
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("RE percentage cannot be negative");
    }

    private Invoice createValidInvoice() {
        return new Invoice(
            1L,
            10L,
            20L,
            "2025-001",
            LocalDateTime.now(),
            new BigDecimal("15.00"),
            new BigDecimal("5.00")
        );
    }

    private InvoiceItem createValidItem() {
        return new InvoiceItem(
            1L,
            1L,
            "Service",
            1,
            new BigDecimal("100.00"),
            new BigDecimal("21.00"),
            BigDecimal.ZERO
        );
    }

    private InvoiceItem createItemWithPrice(BigDecimal price) {
        return new InvoiceItem(
            1L,
            1L,
            "Service",
            1,
            price,
            new BigDecimal("21.00"),
            BigDecimal.ZERO
        );
    }
}
