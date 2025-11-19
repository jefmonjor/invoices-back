package com.invoices.invoice.domain.usecases;

import com.invoices.invoice.domain.ports.PdfGeneratorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GeneratePdfUseCase.
 * Tests PDF generation logic with mocked service.
 */
@ExtendWith(MockitoExtension.class)
class GeneratePdfUseCaseTest {

    @Mock
    private PdfGeneratorService pdfGeneratorService;

    private GeneratePdfUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GeneratePdfUseCase(pdfGeneratorService);
    }

    @Test
    void shouldGeneratePdfSuccessfully() {
        // Arrange
        String invoiceNumber = "2025-001";
        Double baseAmount = 1000.0;
        Double totalAmount = 1210.0;
        byte[] expectedPdf = new byte[]{1, 2, 3};

        when(pdfGeneratorService.generateCustomPdf(
            anyString(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyString(), anyString()
        )).thenReturn(expectedPdf);

        // Act
        byte[] result = useCase.execute(invoiceNumber, baseAmount, 15.0, 5.0, totalAmount, "#FFFFFF", "bold");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(expectedPdf);
        verify(pdfGeneratorService, times(1)).generateCustomPdf(
            eq(invoiceNumber),
            eq(baseAmount),
            eq(15.0),
            eq(5.0),
            eq(totalAmount),
            eq("#FFFFFF"),
            eq("bold")
        );
    }

    @Test
    void shouldUseDefaultValuesForOptionalParameters() {
        // Arrange
        String invoiceNumber = "2025-001";
        Double baseAmount = 1000.0;
        Double totalAmount = 1210.0;
        byte[] expectedPdf = new byte[]{1, 2, 3};

        when(pdfGeneratorService.generateCustomPdf(
            anyString(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyString(), anyString()
        )).thenReturn(expectedPdf);

        // Act
        byte[] result = useCase.execute(invoiceNumber, baseAmount, null, null, totalAmount, null, null);

        // Assert
        assertThat(result).isNotNull();
        verify(pdfGeneratorService, times(1)).generateCustomPdf(
            eq(invoiceNumber),
            eq(baseAmount),
            eq(0.0),
            eq(0.0),
            eq(totalAmount),
            eq("#FFFFFF"),
            eq("normal")
        );
    }

    @Test
    void shouldThrowExceptionWhenInvoiceNumberIsNull() {
        // Act & Assert
        assertThatThrownBy(() ->
            useCase.execute(null, 1000.0, 15.0, 5.0, 1210.0, "#FFFFFF", "bold")
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invoice number cannot be null or empty");

        verify(pdfGeneratorService, never()).generateCustomPdf(
            anyString(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyString(), anyString()
        );
    }

    @Test
    void shouldThrowExceptionWhenInvoiceNumberIsEmpty() {
        // Act & Assert
        assertThatThrownBy(() ->
            useCase.execute("   ", 1000.0, 15.0, 5.0, 1210.0, "#FFFFFF", "bold")
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invoice number cannot be null or empty");

        verify(pdfGeneratorService, never()).generateCustomPdf(
            anyString(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyString(), anyString()
        );
    }

    @Test
    void shouldThrowExceptionWhenBaseAmountIsNull() {
        // Act & Assert
        assertThatThrownBy(() ->
            useCase.execute("2025-001", null, 15.0, 5.0, 1210.0, "#FFFFFF", "bold")
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Base amount must be positive");

        verify(pdfGeneratorService, never()).generateCustomPdf(
            anyString(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyString(), anyString()
        );
    }

    @Test
    void shouldThrowExceptionWhenBaseAmountIsNegative() {
        // Act & Assert
        assertThatThrownBy(() ->
            useCase.execute("2025-001", -100.0, 15.0, 5.0, 1210.0, "#FFFFFF", "bold")
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Base amount must be positive");

        verify(pdfGeneratorService, never()).generateCustomPdf(
            anyString(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyString(), anyString()
        );
    }

    @Test
    void shouldThrowExceptionWhenTotalAmountIsNull() {
        // Act & Assert
        assertThatThrownBy(() ->
            useCase.execute("2025-001", 1000.0, 15.0, 5.0, null, "#FFFFFF", "bold")
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Total amount must be positive");

        verify(pdfGeneratorService, never()).generateCustomPdf(
            anyString(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyString(), anyString()
        );
    }

    @Test
    void shouldThrowExceptionWhenPdfGeneratorServiceIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> new GeneratePdfUseCase(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("PDF Generator Service cannot be null");
    }
}
