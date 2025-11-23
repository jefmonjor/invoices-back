package com.invoices.invoice.infrastructure.services;

import com.invoices.invoice.domain.entities.Client;
import com.invoices.invoice.domain.entities.Company;
import com.invoices.invoice.domain.entities.Invoice;
import com.invoices.invoice.domain.entities.InvoiceItem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PdfGenerationServiceImplTest {

        @Mock
        private TemplateEngine templateEngine;

        private PdfGenerationServiceImpl pdfGenerationService;

        @BeforeEach
        void setUp() {
                pdfGenerationService = new PdfGenerationServiceImpl(templateEngine);
        }

        @Test
        void generateInvoicePdf_ShouldGeneratePdf() {
                // Arrange
                Company company = new Company(
                                1L, "Test Company", "B12345678", "Address", "City", "28000", "Province", "600000000",
                                "email@test.com",
                                "ES0000000000000000000000");

                Client client = new Client(
                                1L, "Test Client", "A87654321", "Address", "City", "28000", "Province", "600000000",
                                "client@test.com", 1L);

                Invoice invoice = new Invoice(
                                1L, company.getId(), client.getId(), "001/2024", LocalDateTime.now(),
                                new BigDecimal("15.00"),
                                new BigDecimal("5.20"));

                InvoiceItem item = new InvoiceItem(
                                1L, 1L, "Test Item", 1, new BigDecimal("100.00"), new BigDecimal("21.00"),
                                BigDecimal.ZERO);

                invoice.addItemInternal(item);
                invoice.setCompany(company);
                invoice.setClient(client);

                // Mock Thymeleaf to return simple valid HTML
                String mockHtml = "<html><body><h1>Factura 001/2024</h1></body></html>";
                when(templateEngine.process(eq("invoice/invoice-template"), any(Context.class)))
                                .thenReturn(mockHtml);

                // Act
                byte[] pdfBytes = pdfGenerationService.generateInvoicePdf(invoice, company, client);

                // Assert
                assertThat(pdfBytes).isNotEmpty();
                // Check PDF header
                assertThat(new String(pdfBytes)).startsWith("%PDF");

                // Verify context variables
                ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
                verify(templateEngine).process(eq("invoice/invoice-template"), contextCaptor.capture());

                Context capturedContext = contextCaptor.getValue();
                assertThat(capturedContext.getVariable("invoice")).isEqualTo(invoice);
                assertThat(capturedContext.getVariable("company")).isEqualTo(company);
                assertThat(capturedContext.getVariable("client")).isEqualTo(client);
                assertThat(capturedContext.getVariable("isTransportInvoice")).isEqualTo(false);
        }
}
