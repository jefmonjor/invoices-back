package com.invoices.invoice.domain.entities;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

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

    @Test
    void shouldSetAndGetExtendedFields() {
        // Arrange
        InvoiceItem item = new InvoiceItem(
            1L,
            100L,
            "Transport service",
            1,
            new BigDecimal("585.00"),
            new BigDecimal("21.00"),
            new BigDecimal("0.00")
        );

        LocalDate itemDate = LocalDate.of(2025, 9, 1);

        // Act
        item.setItemDate(itemDate);
        item.setVehiclePlate("4592JBZ");
        item.setOrderNumber("ORD-123");
        item.setZone("CDF 11");
        item.setGasPercentage(new BigDecimal("5.00"));

        // Assert
        assertThat(item.getItemDate()).isEqualTo(itemDate);
        assertThat(item.getVehiclePlate()).isEqualTo("4592JBZ");
        assertThat(item.getOrderNumber()).isEqualTo("ORD-123");
        assertThat(item.getZone()).isEqualTo("CDF 11");
        assertThat(item.getGasPercentage()).isEqualByComparingTo("5.00");
    }

    @Test
    void shouldThrowExceptionWhenGasPercentageIsNegative() {
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

        // Act & Assert
        assertThatThrownBy(() ->
            item.setGasPercentage(new BigDecimal("-5.00"))
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Gas percentage cannot be negative");
    }

    @Test
    void shouldThrowExceptionWhenGasPercentageExceeds100() {
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

        // Act & Assert
        assertThatThrownBy(() ->
            item.setGasPercentage(new BigDecimal("101.00"))
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Gas percentage cannot exceed 100%");
    }

    @Test
    void shouldAllowNullGasPercentage() {
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
        item.setGasPercentage(null);

        // Assert
        assertThat(item.getGasPercentage()).isNull();
    }
}
