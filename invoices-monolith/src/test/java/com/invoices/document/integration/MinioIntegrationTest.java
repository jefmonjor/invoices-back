package com.invoices.document.integration;

import com.invoices.document.domain.entities.Document;
import com.invoices.document.domain.entities.FileContent;
import com.invoices.document.domain.usecases.*;
import com.invoices.document.presentation.dto.DocumentDTO;
import com.invoices.document.presentation.dto.UploadDocumentResponse;
import com.invoices.document.presentation.mappers.DocumentDtoMapper;
import com.invoices.document.exception.DocumentNotFoundException;
import com.invoices.document.exception.InvalidFileTypeException;
import io.minio.MinioClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for Document Service with real MinIO and PostgreSQL containers.
 * These tests validate the complete flow of document upload, download, and deletion
 * using Testcontainers to ensure proper integration with external services.
 */
@SpringBootTest
@Testcontainers
@Transactional
@Disabled("Integration tests require full infrastructure setup")
class MinioIntegrationTest {

    private static final String MINIO_IMAGE = "minio/minio:RELEASE-2024-01-01T16-36-33Z";
    private static final String POSTGRES_IMAGE = "postgres:16-alpine";

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(POSTGRES_IMAGE)
            .withDatabaseName("documentdb_test")
            .withUsername("testuser")
            .withPassword("testpass");

    @Container
    static GenericContainer<?> minio = new GenericContainer<>(DockerImageName.parse(MINIO_IMAGE))
            .withExposedPorts(9000)
            .withEnv("MINIO_ROOT_USER", "minioadmin")
            .withEnv("MINIO_ROOT_PASSWORD", "minioadmin")
            .withCommand("server", "/data");

    @Autowired
    private UploadDocumentUseCase uploadDocumentUseCase;

    @Autowired
    private DownloadDocumentUseCase downloadDocumentUseCase;

    @Autowired
    private GetDocumentByIdUseCase getDocumentByIdUseCase;

    @Autowired
    private GetDocumentsByInvoiceUseCase getDocumentsByInvoiceUseCase;

    @Autowired
    private DeleteDocumentUseCase deleteDocumentUseCase;

    @Autowired
    private DocumentDtoMapper mapper;

    @Autowired
    private MinioClient minioClient;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // MinIO properties
        registry.add("minio.endpoint", () -> "http://" + minio.getHost() + ":" + minio.getMappedPort(9000));
        registry.add("minio.access-key", () -> "minioadmin");
        registry.add("minio.secret-key", () -> "minioadmin");
        registry.add("minio.bucket-name", () -> "test-invoices");

        // Database properties are auto-configured via @ServiceConnection
    }

    @Test
    void shouldUploadDocumentToMinIOSuccessfully() {
        // Given
        byte[] pdfContent = createValidPdfContent();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-invoice.pdf",
                "application/pdf",
                pdfContent
        );

        // When
        UploadDocumentResponse response = documentService.uploadDocument(file, 1L, "test-user");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getDocumentId()).isNotNull();
        assertThat(response.getFilename()).isEqualTo("test-invoice.pdf");
        assertThat(response.getDownloadUrl()).contains("/api/documents/");
        assertThat(response.getUploadedAt()).isNotNull();
    }

    @Test
    void shouldDownloadDocumentFromMinIOSuccessfully() {
        // Given - upload a document first
        byte[] originalContent = createValidPdfContent();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "download-test.pdf",
                "application/pdf",
                originalContent
        );
        UploadDocumentResponse uploadResponse = documentService.uploadDocument(file, 2L, "test-user");

        // When - download the document
        byte[] downloadedContent = documentService.downloadDocument(uploadResponse.getDocumentId());

        // Then
        assertThat(downloadedContent).isNotNull();
        assertThat(downloadedContent).isEqualTo(originalContent);
    }

    @Test
    void shouldDeleteDocumentFromMinIOSuccessfully() {
        // Given - upload a document first
        byte[] pdfContent = createValidPdfContent();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "delete-test.pdf",
                "application/pdf",
                pdfContent
        );
        UploadDocumentResponse uploadResponse = documentService.uploadDocument(file, 3L, "test-user");
        Long documentId = uploadResponse.getDocumentId();

        // When - delete the document
        documentService.deleteDocument(documentId);

        // Then - should throw exception when trying to get deleted document
        assertThatThrownBy(() -> documentService.getDocumentById(documentId))
                .isInstanceOf(DocumentNotFoundException.class)
                .hasMessageContaining(documentId.toString());
    }

    @Test
    void shouldRejectNonPdfFiles() {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "This is not a PDF".getBytes(StandardCharsets.UTF_8)
        );

        // When/Then
        assertThatThrownBy(() -> documentService.uploadDocument(file, 4L, "test-user"))
                .isInstanceOf(InvalidFileTypeException.class);
    }

    @Test
    void shouldRejectFilesExceedingMaxSize() {
        // Given - create a file larger than 10MB
        byte[] largeContent = new byte[11 * 1024 * 1024]; // 11MB
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large-file.pdf",
                "application/pdf",
                largeContent
        );

        // When/Then
        assertThatThrownBy(() -> documentService.uploadDocument(file, 5L, "test-user"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File size exceeds maximum allowed");
    }

    @Test
    void shouldRejectEmptyFiles() {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.pdf",
                "application/pdf",
                new byte[0]
        );

        // When/Then
        assertThatThrownBy(() -> documentService.uploadDocument(file, 6L, "test-user"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File cannot be empty");
    }

    @Test
    void shouldGetDocumentMetadataSuccessfully() {
        // Given
        byte[] pdfContent = createValidPdfContent();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "metadata-test.pdf",
                "application/pdf",
                pdfContent
        );
        UploadDocumentResponse uploadResponse = documentService.uploadDocument(file, 7L, "test-user");

        // When
        DocumentDTO documentDTO = documentService.getDocumentById(uploadResponse.getDocumentId());

        // Then
        assertThat(documentDTO).isNotNull();
        assertThat(documentDTO.getId()).isEqualTo(uploadResponse.getDocumentId());
        assertThat(documentDTO.getOriginalFilename()).isEqualTo("metadata-test.pdf");
        assertThat(documentDTO.getContentType()).isEqualTo("application/pdf");
        assertThat(documentDTO.getFileSize()).isEqualTo(pdfContent.length);
        assertThat(documentDTO.getInvoiceId()).isEqualTo(7L);
        assertThat(documentDTO.getUploadedBy()).isEqualTo("test-user");
    }

    @Test
    void shouldHandleMultipleUploadsForSameInvoice() {
        // Given
        Long invoiceId = 8L;
        byte[] pdfContent1 = createValidPdfContent();
        byte[] pdfContent2 = createValidPdfContent();

        MockMultipartFile file1 = new MockMultipartFile("file", "doc1.pdf", "application/pdf", pdfContent1);
        MockMultipartFile file2 = new MockMultipartFile("file", "doc2.pdf", "application/pdf", pdfContent2);

        // When
        documentService.uploadDocument(file1, invoiceId, "user1");
        documentService.uploadDocument(file2, invoiceId, "user2");

        // Then
        var documents = documentService.getDocumentsByInvoiceId(invoiceId);
        assertThat(documents).hasSize(2);
        assertThat(documents).extracting(DocumentDTO::getOriginalFilename)
                .containsExactlyInAnyOrder("doc1.pdf", "doc2.pdf");
    }

    @Test
    void shouldGenerateUniqueFilenamesForDuplicateUploads() {
        // Given
        byte[] pdfContent = createValidPdfContent();
        MockMultipartFile file1 = new MockMultipartFile("file", "same.pdf", "application/pdf", pdfContent);
        MockMultipartFile file2 = new MockMultipartFile("file", "same.pdf", "application/pdf", pdfContent);

        // When
        UploadDocumentResponse response1 = documentService.uploadDocument(file1, 9L, "user1");
        UploadDocumentResponse response2 = documentService.uploadDocument(file2, 9L, "user1");

        // Then
        DocumentDTO doc1 = documentService.getDocumentById(response1.getDocumentId());
        DocumentDTO doc2 = documentService.getDocumentById(response2.getDocumentId());

        assertThat(doc1.getFilename()).isNotEqualTo(doc2.getFilename());
        assertThat(doc1.getOriginalFilename()).isEqualTo(doc2.getOriginalFilename());
    }

    /**
     * Creates a minimal valid PDF file content.
     * This is a bare-minimum PDF structure that passes PDF validation.
     */
    private byte[] createValidPdfContent() {
        String pdfContent = "%PDF-1.4\n" +
                "1 0 obj\n" +
                "<< /Type /Catalog /Pages 2 0 R >>\n" +
                "endobj\n" +
                "2 0 obj\n" +
                "<< /Type /Pages /Kids [3 0 R] /Count 1 >>\n" +
                "endobj\n" +
                "3 0 obj\n" +
                "<< /Type /Page /Parent 2 0 R /Resources << /Font << /F1 << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> >> >> " +
                "/MediaBox [0 0 612 792] /Contents 4 0 R >>\n" +
                "endobj\n" +
                "4 0 obj\n" +
                "<< /Length 44 >>\n" +
                "stream\n" +
                "BT\n" +
                "/F1 12 Tf\n" +
                "100 700 Td\n" +
                "(Test Invoice) Tj\n" +
                "ET\n" +
                "endstream\n" +
                "endobj\n" +
                "xref\n" +
                "0 5\n" +
                "0000000000 65535 f\n" +
                "0000000009 00000 n\n" +
                "0000000058 00000 n\n" +
                "0000000115 00000 n\n" +
                "0000000317 00000 n\n" +
                "trailer\n" +
                "<< /Size 5 /Root 1 0 R >>\n" +
                "startxref\n" +
                "410\n" +
                "%%EOF";
        return pdfContent.getBytes(StandardCharsets.UTF_8);
    }
}
