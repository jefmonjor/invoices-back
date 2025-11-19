package com.invoices.invoice;

import com.invoices.invoice.domain.entities.*;
import com.invoices.invoice.domain.ports.ClientRepository;
import com.invoices.invoice.domain.ports.CompanyRepository;
import com.invoices.invoice.domain.ports.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test for Invoice module.
 * Tests the complete flow from repository to database.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class InvoiceIntegrationTest {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private ClientRepository clientRepository;

    private Company testCompany;
    private Client testClient;

    @BeforeEach
    void setUp() {
        // Create and save test company
        testCompany = new Company(
            null,
            "TRANSOLIDO S.L.",
            "B91923755",
            "Castillo Lastrucci, 3, 3D",
            "DOS HERMANAS",
            "41701",
            "SEVILLA",
            "659889201",
            "contacto@transolido.es",
            "ES60 0182 4840 0022 0165 7539"
        );
        testCompany = companyRepository.save(testCompany);

        // Create and save test client
        testClient = new Client(
            null,
            "SERSFRITRUCKS, S.A.",
            "A50008588",
            "JIMÉNEZ DE LA ESPADA, 57, BAJO",
            "CARTAGENA",
            "30203",
            "MURCIA",
            "968123456",
            "info@sersfritrucks.com"
        );
        testClient = clientRepository.save(testClient);
    }

    @Test
    void shouldCreateAndRetrieveInvoice() {
        // Arrange - Create invoice with saved company and client
        Invoice invoice = new Invoice(
            null,
            testCompany.getId(),
            testClient.getId(),
            "INT-2025-001",
            LocalDateTime.now(),
            new BigDecimal("15.00"),
            new BigDecimal("5.00")
        );

        // Add items
        InvoiceItem item1 = new InvoiceItem(
            null,
            null,
            "Consultoría de software",
            10,
            new BigDecimal("100.00"),
            new BigDecimal("21.00"),
            BigDecimal.ZERO
        );

        InvoiceItem item2 = new InvoiceItem(
            null,
            null,
            "Desarrollo backend",
            5,
            new BigDecimal("200.00"),
            new BigDecimal("21.00"),
            new BigDecimal("10.00")
        );

        invoice.addItem(item1);
        invoice.addItem(item2);
        invoice.setNotes("Integration test invoice");

        // Act - Save invoice
        Invoice savedInvoice = invoiceRepository.save(invoice);

        // Assert - Verify saved invoice
        assertThat(savedInvoice).isNotNull();
        assertThat(savedInvoice.getId()).isNotNull();
        assertThat(savedInvoice.getInvoiceNumber()).isEqualTo("INT-2025-001");
        assertThat(savedInvoice.getItems()).hasSize(2);

        // Act - Retrieve invoice
        Optional<Invoice> retrievedInvoice = invoiceRepository.findById(savedInvoice.getId());

        // Assert - Verify retrieved invoice
        assertThat(retrievedInvoice).isPresent();
        assertThat(retrievedInvoice.get().getId()).isEqualTo(savedInvoice.getId());
        assertThat(retrievedInvoice.get().getInvoiceNumber()).isEqualTo("INT-2025-001");
        assertThat(retrievedInvoice.get().getItems()).hasSize(2);
        assertThat(retrievedInvoice.get().getNotes()).isEqualTo("Integration test invoice");
    }

    @Test
    void shouldUpdateInvoice() {
        // Arrange - Create and save invoice
        Invoice invoice = createTestInvoice("INT-2025-002");
        Invoice savedInvoice = invoiceRepository.save(invoice);

        // Act - Update invoice
        savedInvoice.setNotes("Updated notes");
        Invoice updatedInvoice = invoiceRepository.save(savedInvoice);

        // Assert
        assertThat(updatedInvoice.getNotes()).isEqualTo("Updated notes");

        // Verify in database
        Optional<Invoice> retrievedInvoice = invoiceRepository.findById(savedInvoice.getId());
        assertThat(retrievedInvoice).isPresent();
        assertThat(retrievedInvoice.get().getNotes()).isEqualTo("Updated notes");
    }

    @Test
    void shouldDeleteInvoice() {
        // Arrange - Create and save invoice
        Invoice invoice = createTestInvoice("INT-2025-003");
        Invoice savedInvoice = invoiceRepository.save(invoice);
        Long invoiceId = savedInvoice.getId();

        // Act - Delete invoice
        invoiceRepository.deleteById(invoiceId);

        // Assert - Verify deletion
        Optional<Invoice> retrievedInvoice = invoiceRepository.findById(invoiceId);
        assertThat(retrievedInvoice).isEmpty();
    }

    @Test
    void shouldFindAllInvoices() {
        // Arrange - Create and save multiple invoices
        Invoice invoice1 = createTestInvoice("INT-2025-004");
        Invoice invoice2 = createTestInvoice("INT-2025-005");
        Invoice invoice3 = createTestInvoice("INT-2025-006");

        invoiceRepository.save(invoice1);
        invoiceRepository.save(invoice2);
        invoiceRepository.save(invoice3);

        // Act - Find all invoices
        List<Invoice> allInvoices = invoiceRepository.findAll();

        // Assert
        assertThat(allInvoices).isNotNull();
        assertThat(allInvoices).hasSizeGreaterThanOrEqualTo(3);

        // Verify our test invoices are in the list
        assertThat(allInvoices)
            .extracting(Invoice::getInvoiceNumber)
            .contains("INT-2025-004", "INT-2025-005", "INT-2025-006");
    }

    @Test
    void shouldCalculateInvoiceTotals() {
        // Arrange - Create invoice with specific values
        Invoice invoice = new Invoice(
            null,
            testCompany.getId(),
            testClient.getId(),
            "INT-2025-007",
            LocalDateTime.now(),
            new BigDecimal("15.00"),
            new BigDecimal("5.00")
        );

        InvoiceItem item = new InvoiceItem(
            null,
            null,
            "Test item",
            10,
            new BigDecimal("100.00"),
            new BigDecimal("21.00"),
            BigDecimal.ZERO
        );

        invoice.addItem(item);

        // Act - Save invoice
        Invoice savedInvoice = invoiceRepository.save(invoice);

        // Assert - Verify calculations
        // Base amount = 10 * 100 = 1000
        assertThat(savedInvoice.calculateBaseAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));

        // IRPF amount = 1000 * 0.15 = 150
        assertThat(savedInvoice.calculateIrpfAmount()).isEqualByComparingTo(new BigDecimal("150.00"));

        // RE amount = 1000 * 0.05 = 50
        assertThat(savedInvoice.calculateReAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    void shouldVerifyCompanyAndClientExist() {
        // Act & Assert - Check company exists (saved in setUp)
        boolean companyExists = companyRepository.existsById(testCompany.getId());
        assertThat(companyExists).isTrue();

        // Act & Assert - Check client exists (saved in setUp)
        boolean clientExists = clientRepository.existsById(testClient.getId());
        assertThat(clientExists).isTrue();
    }

    @Test
    void shouldHandleInvoiceWithMultipleItems() {
        // Arrange - Create invoice with multiple items
        Invoice invoice = new Invoice(
            null,
            testCompany.getId(),
            testClient.getId(),
            "INT-2025-008",
            LocalDateTime.now(),
            new BigDecimal("15.00"),
            new BigDecimal("5.00")
        );

        for (int i = 1; i <= 5; i++) {
            InvoiceItem item = new InvoiceItem(
                null,
                null,
                "Item " + i,
                i,
                new BigDecimal("50.00"),
                new BigDecimal("21.00"),
                BigDecimal.ZERO
            );
            invoice.addItem(item);
        }

        // Act - Save invoice
        Invoice savedInvoice = invoiceRepository.save(invoice);

        // Assert
        assertThat(savedInvoice.getItems()).hasSize(5);

        // Retrieve and verify
        Optional<Invoice> retrievedInvoice = invoiceRepository.findById(savedInvoice.getId());
        assertThat(retrievedInvoice).isPresent();
        assertThat(retrievedInvoice.get().getItems()).hasSize(5);
    }

    private Invoice createTestInvoice(String invoiceNumber) {
        Invoice invoice = new Invoice(
            null,
            testCompany.getId(),
            testClient.getId(),
            invoiceNumber,
            LocalDateTime.now(),
            new BigDecimal("15.00"),
            new BigDecimal("5.00")
        );

        InvoiceItem item = new InvoiceItem(
            null,
            null,
            "Test item",
            1,
            new BigDecimal("100.00"),
            new BigDecimal("21.00"),
            BigDecimal.ZERO
        );

        invoice.addItem(item);

        return invoice;
    }
}
