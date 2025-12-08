package com.invoices.invoice.infrastructure.persistence;

import com.invoices.invoice.domain.entities.*;
import com.invoices.invoice.domain.models.InvoiceSummary;
import com.invoices.invoice.domain.ports.ClientRepository;
import com.invoices.invoice.domain.ports.CompanyRepository;
import com.invoices.invoice.domain.ports.InvoiceRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
 * Tests to detect N+1 query issues.
 * Uses Hibernate Statistics to count queries executed.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("N+1 Query Detection Tests")
class NPlusOneDetectionTest {

    @org.springframework.boot.test.context.TestConfiguration
    static class TestConfig {
        @org.springframework.context.annotation.Bean
        @org.springframework.context.annotation.Primary
        public io.minio.MinioClient testMinioClient() {
            return org.mockito.Mockito.mock(io.minio.MinioClient.class);
        }

        @org.springframework.context.annotation.Bean
        public com.invoices.document.config.MinioConfig.MinioProperties minioProperties() {
            com.invoices.document.config.MinioConfig.MinioProperties properties = new com.invoices.document.config.MinioConfig.MinioProperties();
            properties.setBucketName("test-bucket");
            properties.setEndpoint("http://localhost:9000");
            properties.setAccessKey("test");
            properties.setSecretKey("test");
            return properties;
        }

        @SuppressWarnings("unchecked")
        @org.springframework.context.annotation.Bean
        public org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate() {
            return org.mockito.Mockito.mock(org.springframework.data.redis.core.RedisTemplate.class);
        }

        @org.springframework.context.annotation.Bean
        @org.springframework.context.annotation.Primary
        public org.springframework.mail.javamail.JavaMailSender javaMailSender() {
            return org.mockito.Mockito.mock(org.springframework.mail.javamail.JavaMailSender.class);
        }
    }

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private ClientRepository clientRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private Statistics hibernateStats;
    private Company testCompany;
    private Client testClient;

    @BeforeEach
    void setUp() {
        // Setup Hibernate statistics
        hibernateStats = entityManager.unwrap(Session.class)
                .getSessionFactory()
                .getStatistics();
        hibernateStats.setStatisticsEnabled(true);

        // Create test company with unique identifiers
        testCompany = new Company(
                null,
                "N+1 Test Company " + System.currentTimeMillis(),
                "B" + (System.currentTimeMillis() % 100000000),
                "Test Address",
                "Test City",
                "12345",
                "Test Province",
                "123456789",
                "nplusone@test.com",
                "ES12345678901234567890");
        testCompany = companyRepository.save(testCompany);

        // Create test client
        testClient = new Client(
                null,
                "N+1 Test Client",
                "A" + (System.currentTimeMillis() % 100000000),
                "Client Address",
                "Client City",
                "54321",
                "Client Province",
                "987654321",
                "client@nplusone.test",
                testCompany.getId());
        testClient = clientRepository.save(testClient);
    }

    @Test
    @DisplayName("Should not generate N+1 queries when listing invoice summaries")
    void shouldNotGenerateNPlusOneQueries_whenListingInvoiceSummaries() {
        // Given - Create multiple invoices
        int invoiceCount = 10;
        for (int i = 0; i < invoiceCount; i++) {
            Invoice invoice = new Invoice(
                    null,
                    testCompany.getId(),
                    testClient.getId(),
                    "N1-" + System.currentTimeMillis() + "-" + i,
                    LocalDateTime.now(),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO);
            invoice.addItem(new InvoiceItem(null, null, "Item " + i, 1,
                    new BigDecimal("100"), new BigDecimal("21"), BigDecimal.ZERO));
            invoiceRepository.save(invoice);
        }

        entityManager.flush();
        entityManager.clear();
        hibernateStats.clear();

        // When
        List<InvoiceSummary> summaries = invoiceRepository.findSummariesByCompanyId(
                testCompany.getId(), 0, 20, null, null);

        // Then
        assertThat(summaries).hasSize(invoiceCount);

        long queryCount = hibernateStats.getQueryExecutionCount();

        // With the fix (using getClientId()), we expect:
        // 1 query for invoices + possibly 1 for count
        // WITHOUT the fix, it would be 1 + N queries (N+1 problem)
        System.out.printf("Query count for %d invoice summaries: %d%n", invoiceCount, queryCount);

        assertThat(queryCount)
                .as("Expected <= 3 queries for %d invoices, but got %d (possible N+1 issue)",
                        invoiceCount, queryCount)
                .isLessThanOrEqualTo(3);
    }

    @Test
    @DisplayName("Should not generate N+1 queries when getting invoice with items")
    void shouldNotGenerateNPlusOneQueries_whenGettingInvoiceWithItems() {
        // Given - Create invoice with multiple items
        int itemCount = 5;
        Invoice invoice = new Invoice(
                null,
                testCompany.getId(),
                testClient.getId(),
                "N1-ITEMS-" + System.currentTimeMillis(),
                LocalDateTime.now(),
                BigDecimal.ZERO,
                BigDecimal.ZERO);

        for (int i = 0; i < itemCount; i++) {
            invoice.addItem(new InvoiceItem(null, null, "Item " + i, i + 1,
                    new BigDecimal("100").add(new BigDecimal(i * 10)),
                    new BigDecimal("21"), BigDecimal.ZERO));
        }
        Invoice saved = invoiceRepository.save(invoice);

        entityManager.flush();
        entityManager.clear();
        hibernateStats.clear();

        // When
        Optional<Invoice> result = invoiceRepository.findById(saved.getId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getItems()).hasSize(itemCount);

        long queryCount = hibernateStats.getQueryExecutionCount();

        // Expected: 1 query for invoice + 1 for items (or 1 if using JOIN FETCH)
        System.out.printf("Query count for invoice with %d items: %d%n", itemCount, queryCount);

        assertThat(queryCount)
                .as("Expected <= 2 queries for invoice with %d items, but got %d (possible N+1)",
                        itemCount, queryCount)
                .isLessThanOrEqualTo(2);
    }

    @Test
    @DisplayName("Should efficiently load multiple invoices by company")
    void shouldEfficientlyLoadMultipleInvoices() {
        // Given
        int invoiceCount = 5;
        for (int i = 0; i < invoiceCount; i++) {
            Invoice invoice = new Invoice(
                    null,
                    testCompany.getId(),
                    testClient.getId(),
                    "EFF-" + System.currentTimeMillis() + "-" + i,
                    LocalDateTime.now(),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO);
            invoice.addItem(new InvoiceItem(null, null, "Item", 1,
                    new BigDecimal("100"), new BigDecimal("21"), BigDecimal.ZERO));
            invoiceRepository.save(invoice);
        }

        entityManager.flush();
        entityManager.clear();
        hibernateStats.clear();

        // When
        List<Invoice> invoices = invoiceRepository.findByCompanyId(testCompany.getId());

        // Then
        assertThat(invoices).hasSize(invoiceCount);

        // Access items to trigger loading
        invoices.forEach(inv -> assertThat(inv.getItems()).isNotEmpty());

        long queryCount = hibernateStats.getQueryExecutionCount();
        System.out.printf("Query count for %d invoices with items access: %d%n",
                invoiceCount, queryCount);

        // Allow more queries here as items might be loaded separately
        assertThat(queryCount)
                .as("Query count seems high, possible N+1: %d queries for %d invoices",
                        queryCount, invoiceCount)
                .isLessThanOrEqualTo(invoiceCount + 2);
    }
}
