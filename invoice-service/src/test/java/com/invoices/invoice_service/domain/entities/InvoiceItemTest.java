package com.invoices.invoice_service.domain.entities;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for InvoiceItem entity.
 * Tests business logic and validations.
 */
class InvoiceItemTest {

    @Test
    void shouldCreateValidInvoiceItem() {
        // Arrange & Act
        InvoiceItem item = new InvoiceItem(
            1L,
            100L,
            "Consulting services",
            2,
            new BigDecimal("500.00"),
            new BigDecimal("21.00"),
            new BigDecimal("0.00")
        );

        // Assert
        assertThat(item.getId()).isEqualTo(1L);
        assertThat(item.getDescription()).isEqualTo("Consulting services");
        assertThat(item.getUnits()).isEqualTo(2);
        assertThat(item.getPrice()).isEqualByComparingTo("500.00");
    }

    @Test
    void shouldCalculateSubtotalCorrectly() {
        // Arrange
        InvoiceItem item = new InvoiceItem(
            1L,
            100L,
            "Service",
            2,
            new BigDecimal("100.00"),
            new BigDecimal("21.00"),
            new BigDecimal("10.00")
        );

        // Act
        BigDecimal subtotal = item.calculateSubtotal();

        // Assert
        assertThat(subtotal).isEqualByComparingTo("180.00");
    }

    @Test
    void shouldCalculateTotalWithVat() {
        // Arrange
        InvoiceItem item = new InvoiceItem(
            1L,
            100L,
            "Service",
            1,
            new BigDecimal("100.00"),
            new BigDecimal("21.00"),
            new BigDecimal("0.00")
        );

        // Act
        BigDecimal total = item.calculateTotal();

        // Assert
        assertThat(total).isEqualByComparingTo("121.00");
    }

    @Test
    void shouldThrowExceptionWhenDescriptionIsNull() {
        // Act & Assert
        assertThatThrownBy(() ->
            new InvoiceItem(1L, 100L, null, 1,
                new BigDecimal("100"), new BigDecimal("21"), new BigDecimal("0"))
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Description cannot be null or empty");
    }

    @Test
    void shouldThrowExceptionWhenDescriptionIsEmpty() {
        // Act & Assert
        assertThatThrownBy(() ->
            new InvoiceItem(1L, 100L, "   ", 1,
                new BigDecimal("100"), new BigDecimal("21"), new BigDecimal("0"))
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Description cannot be null or empty");
    }

    @Test
    void shouldThrowExceptionWhenUnitsAreZero() {
        // Act & Assert
        assertThatThrownBy(() ->
            new InvoiceItem(1L, 100L, "Service", 0,
                new BigDecimal("100"), new BigDecimal("21"), new BigDecimal("0"))
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Units must be positive");
    }

    @Test
    void shouldThrowExceptionWhenUnitsAreNegative() {
        // Act & Assert
        assertThatThrownBy(() ->
            new InvoiceItem(1L, 100L, "Service", -1,
                new BigDecimal("100"), new BigDecimal("21"), new BigDecimal("0"))
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Units must be positive");
    }

    @Test
    void shouldThrowExceptionWhenPriceIsNull() {
        // Act & Assert
        assertThatThrownBy(() ->
            new InvoiceItem(1L, 100L, "Service", 1,
                null, new BigDecimal("21"), new BigDecimal("0"))
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Price must be positive");
    }

    @Test
    void shouldThrowExceptionWhenPriceIsNegative() {
        // Act & Assert
        assertThatThrownBy(() ->
            new InvoiceItem(1L, 100L, "Service", 1,
                new BigDecimal("-10"), new BigDecimal("21"), new BigDecimal("0"))
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Price must be positive");
    }

    @Test
    void shouldThrowExceptionWhenVatIsNegative() {
        // Act & Assert
        assertThatThrownBy(() ->
            new InvoiceItem(1L, 100L, "Service", 1,
                new BigDecimal("100"), new BigDecimal("-5"), new BigDecimal("0"))
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("VAT percentage cannot be negative");
    }

    @Test
    void shouldThrowExceptionWhenDiscountExceeds100Percent() {
        // Act & Assert
        assertThatThrownBy(() ->
            new InvoiceItem(1L, 100L, "Service", 1,
                new BigDecimal("100"), new BigDecimal("21"), new BigDecimal("101"))
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Discount percentage cannot exceed 100%");
    }

    @Test
    void shouldUpdateTimestamp() {
        // Arrange
        InvoiceItem item = new InvoiceItem(
            1L,
            100L,
            "Service",
            1,
            new BigDecimal("100.00"),
            new BigDecimal("21.00"),
            new BigDecimal("0.00")
        );

        // Act
        item.updateTimestamp();

        // Assert
        assertThat(item.getUpdatedAt()).isNotNull();
    }
}
