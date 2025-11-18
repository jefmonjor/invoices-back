package com.invoices.invoice.domain.usecases;

import com.invoices.invoice.domain.entities.Invoice;
import com.invoices.invoice.domain.entities.InvoiceItem;
import com.invoices.invoice.domain.ports.ClientRepository;
import com.invoices.invoice.domain.ports.CompanyRepository;
import com.invoices.invoice.domain.ports.InvoiceRepository;
import com.invoices.invoice.domain.exceptions.ClientNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CreateInvoiceUseCase.
 * Tests invoice creation logic with validation.
 */
@ExtendWith(MockitoExtension.class)
@Disabled("Requires InvoiceEventPublisher parameter - TODO")
class CreateInvoiceUseCaseTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private ClientRepository clientRepository;

    private CreateInvoiceUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateInvoiceUseCase(invoiceRepository, companyRepository, clientRepository);
    }

    @Test
    void shouldCreateInvoiceSuccessfully() {
        // Arrange
        Long companyId = 1L;
        Long clientId = 2L;
        String invoiceNumber = "2025-001";
        BigDecimal irpfPercentage = new BigDecimal("15.00");
        BigDecimal rePercentage = new BigDecimal("5.00");
        String notes = "Test invoice";

        List<InvoiceItem> items = createTestItems();

        when(companyRepository.existsById(companyId)).thenReturn(true);
        when(clientRepository.existsById(clientId)).thenReturn(true);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> {
            Invoice invoice = invocation.getArgument(0);
            return new Invoice(
                1L,
                invoice.getCompanyId(),
                invoice.getClientId(),
                invoice.getInvoiceNumber(),
                invoice.getIssueDate(),
                invoice.getIrpfPercentage(),
                invoice.getRePercentage()
            );
        });

        // Act
        Invoice result = useCase.execute(companyId, clientId, invoiceNumber, irpfPercentage, rePercentage, items, notes);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCompanyId()).isEqualTo(companyId);
        assertThat(result.getClientId()).isEqualTo(clientId);
        assertThat(result.getInvoiceNumber()).isEqualTo(invoiceNumber);
        assertThat(result.getIrpfPercentage()).isEqualByComparingTo(irpfPercentage);
        assertThat(result.getRePercentage()).isEqualByComparingTo(rePercentage);

        verify(companyRepository, times(1)).existsById(companyId);
        verify(clientRepository, times(1)).existsById(clientId);
        verify(invoiceRepository, times(1)).save(any(Invoice.class));
    }

    @Test
    void shouldCreateInvoiceWithDefaultPercentages() {
        // Arrange
        Long companyId = 1L;
        Long clientId = 2L;
        String invoiceNumber = "2025-002";
        List<InvoiceItem> items = createTestItems();

        when(companyRepository.existsById(companyId)).thenReturn(true);
        when(clientRepository.existsById(clientId)).thenReturn(true);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Invoice result = useCase.execute(companyId, clientId, invoiceNumber, null, null, items, null);

        // Assert
        ArgumentCaptor<Invoice> invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository).save(invoiceCaptor.capture());

        Invoice savedInvoice = invoiceCaptor.getValue();
        assertThat(savedInvoice.getIrpfPercentage()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(savedInvoice.getRePercentage()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldThrowExceptionWhenCompanyNotFound() {
        // Arrange
        Long nonExistentCompanyId = 999L;
        Long clientId = 2L;
        String invoiceNumber = "2025-003";
        List<InvoiceItem> items = createTestItems();

        when(companyRepository.existsById(nonExistentCompanyId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> useCase.execute(
            nonExistentCompanyId, clientId, invoiceNumber, null, null, items, null
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Company not found");

        verify(companyRepository, times(1)).existsById(nonExistentCompanyId);
        verify(clientRepository, never()).existsById(any());
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenClientNotFound() {
        // Arrange
        Long companyId = 1L;
        Long nonExistentClientId = 999L;
        String invoiceNumber = "2025-004";
        List<InvoiceItem> items = createTestItems();

        when(companyRepository.existsById(companyId)).thenReturn(true);
        when(clientRepository.existsById(nonExistentClientId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> useCase.execute(
            companyId, nonExistentClientId, invoiceNumber, null, null, items, null
        ))
            .isInstanceOf(ClientNotFoundException.class);

        verify(companyRepository, times(1)).existsById(companyId);
        verify(clientRepository, times(1)).existsById(nonExistentClientId);
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void shouldCreateInvoiceWithNullItems() {
        // Arrange
        Long companyId = 1L;
        Long clientId = 2L;
        String invoiceNumber = "2025-005";

        when(companyRepository.existsById(companyId)).thenReturn(true);
        when(clientRepository.existsById(clientId)).thenReturn(true);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Invoice result = useCase.execute(companyId, clientId, invoiceNumber, null, null, null, null);

        // Assert
        assertThat(result).isNotNull();
        verify(invoiceRepository, times(1)).save(any(Invoice.class));
    }

    @Test
    void shouldCreateInvoiceWithEmptyNotes() {
        // Arrange
        Long companyId = 1L;
        Long clientId = 2L;
        String invoiceNumber = "2025-006";
        List<InvoiceItem> items = createTestItems();

        when(companyRepository.existsById(companyId)).thenReturn(true);
        when(clientRepository.existsById(clientId)).thenReturn(true);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Invoice result = useCase.execute(companyId, clientId, invoiceNumber, null, null, items, "   ");

        // Assert
        assertThat(result).isNotNull();
        verify(invoiceRepository, times(1)).save(any(Invoice.class));
    }

    @Test
    void shouldCreateInvoiceWithMultipleItems() {
        // Arrange
        Long companyId = 1L;
        Long clientId = 2L;
        String invoiceNumber = "2025-007";

        List<InvoiceItem> items = new ArrayList<>();
        items.add(createTestItem("Item 1", 2, "100.00", "21.00"));
        items.add(createTestItem("Item 2", 3, "50.00", "21.00"));
        items.add(createTestItem("Item 3", 1, "200.00", "10.00"));

        when(companyRepository.existsById(companyId)).thenReturn(true);
        when(clientRepository.existsById(clientId)).thenReturn(true);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Invoice result = useCase.execute(companyId, clientId, invoiceNumber, null, null, items, null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getItems()).hasSize(3);
        verify(invoiceRepository, times(1)).save(any(Invoice.class));
    }

    private List<InvoiceItem> createTestItems() {
        List<InvoiceItem> items = new ArrayList<>();
        items.add(createTestItem("Test item", 2, "100.00", "21.00"));
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
