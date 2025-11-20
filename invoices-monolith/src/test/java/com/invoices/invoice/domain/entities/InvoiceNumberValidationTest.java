package com.invoices.invoice.domain.entities;

import com.invoices.invoice.domain.exceptions.InvalidInvoiceNumberFormatException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for Invoice number validation.
 * Tests the new flexible invoice number format pattern.
 */
class InvoiceNumberValidationTest {

    @Test
    void shouldAcceptTraditionalFormat() {
        // Act & Assert - Should not throw
        assertThatNoException().isThrownBy(() ->
            new Invoice(null, 1L, 2L, "2025-001", LocalDateTime.now(), BigDecimal.ZERO, BigDecimal.ZERO)
        );
    }

    @Test
    void shouldAcceptPrefixedFormat() {
        // Act & Assert - Should not throw
        assertThatNoException().isThrownBy(() ->
            new Invoice(null, 1L, 2L, "INV-2025-001", LocalDateTime.now(), BigDecimal.ZERO, BigDecimal.ZERO)
        );
    }

    @Test
    void shouldAcceptSlashFormatShortYear() {
        // Act & Assert - Should not throw
        assertThatNoException().isThrownBy(() ->
            new Invoice(null, 1L, 2L, "047/2025", LocalDateTime.now(), BigDecimal.ZERO, BigDecimal.ZERO)
        );
    }

    @Test
    void shouldAcceptSlashFormatWithPrefix() {
        // Act & Assert - Should not throw
        assertThatNoException().isThrownBy(() ->
            new Invoice(null, 1L, 2L, "A057/2025", LocalDateTime.now(), BigDecimal.ZERO, BigDecimal.ZERO)
        );
    }

    @Test
    void shouldAcceptPdfFilenameFormat() {
        // Act & Assert - Should not throw
        assertThatNoException().isThrownBy(() ->
            new Invoice(null, 1L, 2L, "FacturaA057.pdf", LocalDateTime.now(), BigDecimal.ZERO, BigDecimal.ZERO)
        );
    }

    @Test
    void shouldAcceptComplexFormatWithDashes() {
        // Act & Assert - Should not throw
        assertThatNoException().isThrownBy(() ->
            new Invoice(null, 1L, 2L, "4592JBZ-SEP-25", LocalDateTime.now(), BigDecimal.ZERO, BigDecimal.ZERO)
        );
    }

    @Test
    void shouldAcceptMixedCaseLetters() {
        // Act & Assert - Should not throw
        assertThatNoException().isThrownBy(() ->
            new Invoice(null, 1L, 2L, "Factura123", LocalDateTime.now(), BigDecimal.ZERO, BigDecimal.ZERO)
        );
    }

    @Test
    void shouldAcceptOnlyNumbers() {
        // Act & Assert - Should not throw
        assertThatNoException().isThrownBy(() ->
            new Invoice(null, 1L, 2L, "20250001", LocalDateTime.now(), BigDecimal.ZERO, BigDecimal.ZERO)
        );
    }

    @Test
    void shouldAcceptOnlyLetters() {
        // Act & Assert - Should not throw
        assertThatNoException().isThrownBy(() ->
            new Invoice(null, 1L, 2L, "FACTURA", LocalDateTime.now(), BigDecimal.ZERO, BigDecimal.ZERO)
        );
    }

    @Test
    void shouldAcceptDotsInNumber() {
        // Act & Assert - Should not throw
        assertThatNoException().isThrownBy(() ->
            new Invoice(null, 1L, 2L, "2025.001.A", LocalDateTime.now(), BigDecimal.ZERO, BigDecimal.ZERO)
        );
    }

    @Test
    void shouldRejectNullInvoiceNumber() {
        // Act & Assert
        assertThatThrownBy(() ->
            new Invoice(null, 1L, 2L, null, LocalDateTime.now(), BigDecimal.ZERO, BigDecimal.ZERO)
        )
        .isInstanceOf(InvalidInvoiceNumberFormatException.class)
        .hasMessageContaining("null or empty");
    }

    @Test
    void shouldRejectEmptyInvoiceNumber() {
        // Act & Assert
        assertThatThrownBy(() ->
            new Invoice(null, 1L, 2L, "", LocalDateTime.now(), BigDecimal.ZERO, BigDecimal.ZERO)
        )
        .isInstanceOf(InvalidInvoiceNumberFormatException.class)
        .hasMessageContaining("null or empty");
    }

    @Test
    void shouldRejectBlankInvoiceNumber() {
        // Act & Assert
        assertThatThrownBy(() ->
            new Invoice(null, 1L, 2L, "   ", LocalDateTime.now(), BigDecimal.ZERO, BigDecimal.ZERO)
        )
        .isInstanceOf(InvalidInvoiceNumberFormatException.class)
        .hasMessageContaining("null or empty");
    }

    @Test
    void shouldRejectInvoiceNumberWithSpaces() {
        // Act & Assert
        assertThatThrownBy(() ->
            new Invoice(null, 1L, 2L, "INV 2025-001", LocalDateTime.now(), BigDecimal.ZERO, BigDecimal.ZERO)
        )
        .isInstanceOf(InvalidInvoiceNumberFormatException.class)
        .hasMessageContaining("INV 2025-001");
    }

    @Test
    void shouldRejectInvoiceNumberWithSpecialCharacters() {
        // Act & Assert
        assertThatThrownBy(() ->
            new Invoice(null, 1L, 2L, "INV@2025", LocalDateTime.now(), BigDecimal.ZERO, BigDecimal.ZERO)
        )
        .isInstanceOf(InvalidInvoiceNumberFormatException.class)
        .hasMessageContaining("INV@2025");
    }

    @Test
    void shouldRejectInvoiceNumberWithUnderscore() {
        // Act & Assert
        assertThatThrownBy(() ->
            new Invoice(null, 1L, 2L, "INV_2025_001", LocalDateTime.now(), BigDecimal.ZERO, BigDecimal.ZERO)
        )
        .isInstanceOf(InvalidInvoiceNumberFormatException.class)
        .hasMessageContaining("INV_2025_001");
    }

    @Test
    void shouldRejectInvoiceNumberWithHash() {
        // Act & Assert
        assertThatThrownBy(() ->
            new Invoice(null, 1L, 2L, "#2025-001", LocalDateTime.now(), BigDecimal.ZERO, BigDecimal.ZERO)
        )
        .isInstanceOf(InvalidInvoiceNumberFormatException.class)
        .hasMessageContaining("#2025-001");
    }
}
