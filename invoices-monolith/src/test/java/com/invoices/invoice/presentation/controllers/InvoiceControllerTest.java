package com.invoices.invoice.presentation.controllers;

import com.invoices.invoice.dto.InvoiceDTO;
import com.invoices.invoice.domain.entities.*;
import com.invoices.invoice.domain.exceptions.InvoiceNotFoundException;
import com.invoices.invoice.domain.usecases.GeneratePdfUseCase;
import com.invoices.invoice.domain.usecases.GetInvoiceByIdUseCase;
import com.invoices.invoice.presentation.mappers.InvoiceDtoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for InvoiceController.
 * Tests REST API endpoints without starting the server.
 */
@ExtendWith(MockitoExtension.class)
@Disabled("Requires refactoring after Clean Architecture migration - TODO")
class InvoiceControllerTest {

    @Mock
    private GetInvoiceByIdUseCase getInvoiceByIdUseCase;

    @Mock
    private GeneratePdfUseCase generatePdfUseCase;

    @Mock
    private InvoiceDtoMapper dtoMapper;

    private InvoiceController invoiceController;

    @BeforeEach
    void setUp() {
        invoiceController = new InvoiceController(
            getInvoiceByIdUseCase,
            generatePdfUseCase,
            dtoMapper
        );
    }

    @Test
    void shouldReturnInvoiceWhenIdExists() {
        // Arrange
        Integer invoiceId = 1;
        Invoice invoice = createTestInvoice();
        InvoiceDTO invoiceDTO = new InvoiceDTO();

        when(getInvoiceByIdUseCase.execute(invoiceId.longValue())).thenReturn(invoice);
        when(dtoMapper.toDto(invoice)).thenReturn(invoiceDTO);

        // Act
        ResponseEntity<InvoiceDTO> response = invoiceController.invoicesIdGet(invoiceId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(invoiceDTO);
        verify(getInvoiceByIdUseCase, times(1)).execute(invoiceId.longValue());
        verify(dtoMapper, times(1)).toDto(invoice);
    }

    @Test
    void shouldReturnNotFoundWhenInvoiceDoesNotExist() {
        // Arrange
        Integer invoiceId = 999;

        when(getInvoiceByIdUseCase.execute(invoiceId.longValue()))
            .thenThrow(new InvoiceNotFoundException(invoiceId.longValue()));

        // Act
        ResponseEntity<InvoiceDTO> response = invoiceController.invoicesIdGet(invoiceId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
        verify(getInvoiceByIdUseCase, times(1)).execute(invoiceId.longValue());
        verify(dtoMapper, never()).toDto(any());
    }

    @Test
    void shouldReturnBadRequestWhenIdIsInvalid() {
        // Arrange
        Integer invoiceId = -1;

        when(getInvoiceByIdUseCase.execute(invoiceId.longValue()))
            .thenThrow(new IllegalArgumentException("Invalid ID"));

        // Act
        ResponseEntity<InvoiceDTO> response = invoiceController.invoicesIdGet(invoiceId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void shouldReturnInternalServerErrorWhenUnexpectedExceptionOccurs() {
        // Arrange
        Integer invoiceId = 1;

        when(getInvoiceByIdUseCase.execute(invoiceId.longValue()))
            .thenThrow(new RuntimeException("Unexpected error"));

        // Act
        ResponseEntity<InvoiceDTO> response = invoiceController.invoicesIdGet(invoiceId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void shouldGeneratePdfSuccessfully() {
        // Arrange
        InvoiceConfigDTO configDTO = new InvoiceConfigDTO();
        configDTO.setInvoiceNumber("2025-001");
        configDTO.setBaseAmount(BigDecimal.valueOf(1000.00));
        configDTO.setIrpfPercentage(BigDecimal.valueOf(15.00));
        configDTO.setRePercentage(BigDecimal.valueOf(5.00));
        configDTO.setTotalAmount(BigDecimal.valueOf(1210.00));
        configDTO.setColor("#FFFFFF");
        configDTO.setTextStyle("bold");

        byte[] mockPdfBytes = "%PDF-1.4 mock content".getBytes();

        when(generatePdfUseCase.execute(
            anyString(),
            anyDouble(),
            anyDouble(),
            anyDouble(),
            anyDouble(),
            anyString(),
            anyString()
        )).thenReturn(mockPdfBytes);

        // Act
        ResponseEntity<org.springframework.core.io.Resource> response =
            invoiceController.invoicesGeneratePdfPost(configDTO);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getHeaders().getContentType()).isEqualTo(
            org.springframework.http.MediaType.APPLICATION_PDF
        );
        assertThat(response.getHeaders().getContentDisposition().toString())
            .contains("attachment")
            .contains("invoice.pdf");
    }

    @Test
    void shouldGeneratePdfWithNullOptionalParameters() {
        // Arrange
        InvoiceConfigDTO configDTO = new InvoiceConfigDTO();
        configDTO.setInvoiceNumber("2025-002");
        configDTO.setBaseAmount(BigDecimal.valueOf(1000.00));
        configDTO.setIrpfPercentage(null);
        configDTO.setRePercentage(null);
        configDTO.setTotalAmount(BigDecimal.valueOf(1000.00));
        configDTO.setColor(null);
        configDTO.setTextStyle(null);

        byte[] mockPdfBytes = "%PDF-1.4 mock content".getBytes();

        when(generatePdfUseCase.execute(
            anyString(),
            anyDouble(),
            isNull(),
            isNull(),
            anyDouble(),
            isNull(),
            isNull()
        )).thenReturn(mockPdfBytes);

        // Act
        ResponseEntity<org.springframework.core.io.Resource> response =
            invoiceController.invoicesGeneratePdfPost(configDTO);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void shouldReturnBadRequestWhenPdfGenerationFails() {
        // Arrange
        InvoiceConfigDTO configDTO = new InvoiceConfigDTO();
        configDTO.setInvoiceNumber("INVALID");
        configDTO.setBaseAmount(BigDecimal.valueOf(-100.00));
        configDTO.setTotalAmount(BigDecimal.valueOf(0.00));

        when(generatePdfUseCase.execute(
            anyString(),
            anyDouble(),
            any(),
            any(),
            anyDouble(),
            any(),
            any()
        )).thenThrow(new IllegalArgumentException("Invalid input"));

        // Act
        ResponseEntity<org.springframework.core.io.Resource> response =
            invoiceController.invoicesGeneratePdfPost(configDTO);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void shouldReturnInternalServerErrorWhenPdfGenerationThrowsException() {
        // Arrange
        InvoiceConfigDTO configDTO = new InvoiceConfigDTO();
        configDTO.setInvoiceNumber("2025-003");
        configDTO.setBaseAmount(BigDecimal.valueOf(1000.00));
        configDTO.setTotalAmount(BigDecimal.valueOf(1000.00));

        when(generatePdfUseCase.execute(
            anyString(),
            anyDouble(),
            any(),
            any(),
            anyDouble(),
            any(),
            any()
        )).thenThrow(new RuntimeException("PDF generation error"));

        // Act
        ResponseEntity<org.springframework.core.io.Resource> response =
            invoiceController.invoicesGeneratePdfPost(configDTO);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNull();
    }

    private Invoice createTestInvoice() {
        Invoice invoice = new Invoice(
            1L,
            1L,
            1L,
            "2025-001",
            LocalDateTime.now(),
            BigDecimal.valueOf(15),
            BigDecimal.valueOf(5)
        );

        Company company = new Company(
            1L,
            "Test Company",
            "B12345678",
            "Address",
            "City",
            "12345",
            "Province",
            "123456789",
            "test@test.com",
            "ES1234567890"
        );

        Client client = new Client(
            1L,
            "Test Client",
            "A12345678",
            "Client Address",
            "City",
            "12345",
            "Province",
            "987654321",
            "client@test.com"
        );

        invoice.setCompany(company);
        invoice.setClient(client);

        InvoiceItem item = new InvoiceItem(
            1L,
            1L,
            "Test Item",
            1,
            BigDecimal.valueOf(100.00),
            BigDecimal.valueOf(21.00),
            BigDecimal.ZERO
        );

        invoice.addItem(item);

        return invoice;
    }
}
