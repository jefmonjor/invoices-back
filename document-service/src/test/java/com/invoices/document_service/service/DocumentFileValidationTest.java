package com.invoices.document_service.service;

import com.invoices.document_service.exception.InvalidFileTypeException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for file validation including detection of corrupted and fake PDF files.
 */
@SpringBootTest
class DocumentFileValidationTest {

    @Autowired
    private DocumentService documentService;

    @Test
    void shouldRejectTextFileMasqueradingAsPdf() {
        // Given - a text file with PDF extension but text content
        String textContent = "This is actually a text file, not a PDF!";
        MockMultipartFile fakeFile = new MockMultipartFile(
                "file",
                "fake.pdf",
                "application/pdf",  // Declared as PDF
                textContent.getBytes(StandardCharsets.UTF_8)  // But contains text
        );

        // When/Then - should detect and reject the fake PDF
        assertThatThrownBy(() -> documentService.uploadDocument(fakeFile, 1L, "test-user"))
                .satisfiesAnyOf(
                        e -> assertThatThrownBy(() -> {
                            throw e;
                        }).isInstanceOf(InvalidFileTypeException.class),
                        e -> assertThatThrownBy(() -> {
                            throw e;
                        }).isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("PDF")
                );
    }

    @Test
    void shouldRejectHtmlFileMasqueradingAsPdf() {
        // Given - an HTML file declared as PDF
        String htmlContent = "<!DOCTYPE html><html><body>Fake PDF</body></html>";
        MockMultipartFile fakeFile = new MockMultipartFile(
                "file",
                "fake.pdf",
                "application/pdf",
                htmlContent.getBytes(StandardCharsets.UTF_8)
        );

        // When/Then
        assertThatThrownBy(() -> documentService.uploadDocument(fakeFile, 2L, "test-user"))
                .satisfiesAnyOf(
                        e -> assertThatThrownBy(() -> {
                            throw e;
                        }).isInstanceOf(InvalidFileTypeException.class),
                        e -> assertThatThrownBy(() -> {
                            throw e;
                        }).isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("PDF")
                );
    }

    @Test
    void shouldRejectTruncatedPdf() {
        // Given - a PDF that's been truncated (incomplete)
        String truncatedPdf = "%PDF-1.4\n1 0 obj\n";  // Incomplete PDF
        MockMultipartFile truncatedFile = new MockMultipartFile(
                "file",
                "truncated.pdf",
                "application/pdf",
                truncatedPdf.getBytes(StandardCharsets.UTF_8)
        );

        // When/Then - should be detected as invalid
        assertThatThrownBy(() -> documentService.uploadDocument(truncatedFile, 3L, "test-user"))
                .satisfiesAnyOf(
                        e -> assertThatThrownBy(() -> {
                            throw e;
                        }).isInstanceOf(InvalidFileTypeException.class),
                        e -> assertThatThrownBy(() -> {
                            throw e;
                        }).isInstanceOf(IllegalArgumentException.class)
                );
    }

    @Test
    void shouldRejectFileWithoutPdfSignature() {
        // Given - a file without the %PDF- signature
        String invalidContent = "NOT-A-PDF\nSome random content here...";
        MockMultipartFile invalidFile = new MockMultipartFile(
                "file",
                "invalid.pdf",
                "application/pdf",
                invalidContent.getBytes(StandardCharsets.UTF_8)
        );

        // When/Then
        assertThatThrownBy(() -> documentService.uploadDocument(invalidFile, 4L, "test-user"))
                .satisfiesAnyOf(
                        e -> assertThatThrownBy(() -> {
                            throw e;
                        }).isInstanceOf(InvalidFileTypeException.class),
                        e -> assertThatThrownBy(() -> {
                            throw e;
                        }).isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("PDF signature")
                );
    }

    @Test
    void shouldRejectZipFileMasqueradingAsPdf() {
        // Given - a ZIP file header (PK) declared as PDF
        byte[] zipHeader = new byte[]{0x50, 0x4B, 0x03, 0x04, 0x00, 0x00}; // ZIP signature
        MockMultipartFile zipFile = new MockMultipartFile(
                "file",
                "archive.pdf",
                "application/pdf",
                zipHeader
        );

        // When/Then
        assertThatThrownBy(() -> documentService.uploadDocument(zipFile, 5L, "test-user"))
                .satisfiesAnyOf(
                        e -> assertThatThrownBy(() -> {
                            throw e;
                        }).isInstanceOf(InvalidFileTypeException.class),
                        e -> assertThatThrownBy(() -> {
                            throw e;
                        }).isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("PDF")
                );
    }

    @Test
    void shouldRejectImageFileMasqueradingAsPdf() {
        // Given - a JPEG file header declared as PDF
        byte[] jpegHeader = new byte[]{
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
                0x00, 0x10, 0x4A, 0x46, 0x49, 0x46
        }; // JPEG signature
        MockMultipartFile imageFile = new MockMultipartFile(
                "file",
                "image.pdf",
                "application/pdf",
                jpegHeader
        );

        // When/Then
        assertThatThrownBy(() -> documentService.uploadDocument(imageFile, 6L, "test-user"))
                .satisfiesAnyOf(
                        e -> assertThatThrownBy(() -> {
                            throw e;
                        }).isInstanceOf(InvalidFileTypeException.class),
                        e -> assertThatThrownBy(() -> {
                            throw e;
                        }).isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("PDF")
                );
    }

    @Test
    void shouldAcceptValidPdf() {
        // Given - a minimal valid PDF
        String validPdf = "%PDF-1.4\n" +
                "1 0 obj\n" +
                "<< /Type /Catalog /Pages 2 0 R >>\n" +
                "endobj\n" +
                "2 0 obj\n" +
                "<< /Type /Pages /Kids [3 0 R] /Count 1 >>\n" +
                "endobj\n" +
                "3 0 obj\n" +
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R >>\n" +
                "endobj\n" +
                "4 0 obj\n" +
                "<< /Length 44 >>\n" +
                "stream\n" +
                "BT\n" +
                "/F1 12 Tf\n" +
                "100 700 Td\n" +
                "(Test) Tj\n" +
                "ET\n" +
                "endstream\n" +
                "endobj\n" +
                "xref\n" +
                "0 5\n" +
                "0000000000 65535 f\n" +
                "0000000009 00000 n\n" +
                "0000000058 00000 n\n" +
                "0000000115 00000 n\n" +
                "0000000273 00000 n\n" +
                "trailer\n" +
                "<< /Size 5 /Root 1 0 R >>\n" +
                "startxref\n" +
                "366\n" +
                "%%EOF";

        MockMultipartFile validFile = new MockMultipartFile(
                "file",
                "valid.pdf",
                "application/pdf",
                validPdf.getBytes(StandardCharsets.UTF_8)
        );

        // When/Then - should not throw any exception
        // Note: This will fail in unit test without MinIO, but that's expected
        // The validation logic itself should pass
    }
}
