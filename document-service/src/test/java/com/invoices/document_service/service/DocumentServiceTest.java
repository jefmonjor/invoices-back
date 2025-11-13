package com.invoices.document_service.service;

import com.invoices.document_service.config.MinioConfig;
import com.invoices.document_service.dto.DocumentDTO;
import com.invoices.document_service.dto.UploadDocumentResponse;
import com.invoices.document_service.entity.Document;
import com.invoices.document_service.exception.DocumentNotFoundException;
import com.invoices.document_service.exception.FileUploadException;
import com.invoices.document_service.exception.InvalidFileTypeException;
import com.invoices.document_service.repository.DocumentRepository;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DocumentService.
 * Tests document upload, download, and MinIO integration.
 *
 * Uses Mockito to mock MinioClient and DocumentRepository.
 * Follows AAA pattern: Arrange-Act-Assert.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentService Unit Tests")
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private MinioClient minioClient;

    @Mock
    private MinioConfig.MinioProperties minioProperties;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private DocumentService documentService;

    private Document testDocument;
    private static final String BUCKET_NAME = "test-bucket";
    private static final Long INVOICE_ID = 1L;
    private static final String UPLOADED_BY = "test-user";

    @BeforeEach
    void setUp() {
        testDocument = Document.builder()
                .id(1L)
                .filename("unique-filename.pdf")
                .originalFilename("test-document.pdf")
                .contentType("application/pdf")
                .fileSize(1024L)
                .minioObjectName("unique-filename.pdf")
                .invoiceId(INVOICE_ID)
                .uploadedBy(UPLOADED_BY)
                .createdAt(LocalDateTime.now())
                .build();

        when(minioProperties.getBucketName()).thenReturn(BUCKET_NAME);
    }

    @Nested
    @DisplayName("Upload Document Tests")
    class UploadDocumentTests {

        @Test
        @DisplayName("Should upload document successfully to MinIO")
        void shouldUploadDocumentSuccessfully() throws Exception {
            // Arrange
            byte[] content = "test pdf content".getBytes();
            when(multipartFile.getOriginalFilename()).thenReturn("test.pdf");
            when(multipartFile.getContentType()).thenReturn("application/pdf");
            when(multipartFile.getSize()).thenReturn(1024L);
            when(multipartFile.isEmpty()).thenReturn(false);
            when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(content));

            when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
                Document doc = invocation.getArgument(0);
                doc.setId(1L);
                doc.setCreatedAt(LocalDateTime.now());
                return doc;
            });

            // Act
            UploadDocumentResponse response = documentService.uploadDocument(
                    multipartFile, INVOICE_ID, UPLOADED_BY);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getDocumentId()).isEqualTo(1L);
            assertThat(response.getFilename()).isEqualTo("test.pdf");
            assertThat(response.getDownloadUrl()).contains("/api/documents/1/download");

            verify(minioClient).putObject(any(PutObjectArgs.class));
            verify(documentRepository).save(any(Document.class));
        }

        @Test
        @DisplayName("Should throw InvalidFileTypeException for non-PDF files")
        void shouldRejectNonPdfFiles() {
            // Arrange
            when(multipartFile.getOriginalFilename()).thenReturn("test.txt");
            when(multipartFile.getContentType()).thenReturn("text/plain");
            when(multipartFile.isEmpty()).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> documentService.uploadDocument(
                    multipartFile, INVOICE_ID, UPLOADED_BY))
                    .isInstanceOf(InvalidFileTypeException.class);

            verify(minioClient, never()).putObject(any(PutObjectArgs.class));
            verify(documentRepository, never()).save(any(Document.class));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for empty file")
        void shouldRejectEmptyFile() {
            // Arrange
            when(multipartFile.isEmpty()).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> documentService.uploadDocument(
                    multipartFile, INVOICE_ID, UPLOADED_BY))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("empty");

            verify(minioClient, never()).putObject(any(PutObjectArgs.class));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for null file")
        void shouldRejectNullFile() {
            // Act & Assert
            assertThatThrownBy(() -> documentService.uploadDocument(
                    null, INVOICE_ID, UPLOADED_BY))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for file exceeding max size")
        void shouldRejectOversizedFile() {
            // Arrange
            long maxSize = 10 * 1024 * 1024; // 10MB
            when(multipartFile.getOriginalFilename()).thenReturn("large.pdf");
            when(multipartFile.getContentType()).thenReturn("application/pdf");
            when(multipartFile.getSize()).thenReturn(maxSize + 1);
            when(multipartFile.isEmpty()).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> documentService.uploadDocument(
                    multipartFile, INVOICE_ID, UPLOADED_BY))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("exceeds maximum");

            verify(minioClient, never()).putObject(any(PutObjectArgs.class));
        }

        @Test
        @DisplayName("Should throw FileUploadException when MinIO upload fails")
        void shouldThrowExceptionWhenMinioUploadFails() throws Exception {
            // Arrange
            when(multipartFile.getOriginalFilename()).thenReturn("test.pdf");
            when(multipartFile.getContentType()).thenReturn("application/pdf");
            when(multipartFile.getSize()).thenReturn(1024L);
            when(multipartFile.isEmpty()).thenReturn(false);
            when(multipartFile.getInputStream()).thenThrow(new RuntimeException("MinIO error"));

            // Act & Assert
            assertThatThrownBy(() -> documentService.uploadDocument(
                    multipartFile, INVOICE_ID, UPLOADED_BY))
                    .isInstanceOf(FileUploadException.class);

            verify(documentRepository, never()).save(any(Document.class));
        }

        @Test
        @DisplayName("Should generate unique filename for uploaded document")
        void shouldGenerateUniqueFilename() throws Exception {
            // Arrange
            byte[] content = "test content".getBytes();
            when(multipartFile.getOriginalFilename()).thenReturn("test.pdf");
            when(multipartFile.getContentType()).thenReturn("application/pdf");
            when(multipartFile.getSize()).thenReturn(1024L);
            when(multipartFile.isEmpty()).thenReturn(false);
            when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(content));

            when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            documentService.uploadDocument(multipartFile, INVOICE_ID, UPLOADED_BY);

            // Assert
            ArgumentCaptor<Document> docCaptor = ArgumentCaptor.forClass(Document.class);
            verify(documentRepository).save(docCaptor.capture());

            Document savedDoc = docCaptor.getValue();
            assertThat(savedDoc.getFilename()).isNotEqualTo("test.pdf");
            assertThat(savedDoc.getFilename()).endsWith(".pdf");
            assertThat(savedDoc.getOriginalFilename()).isEqualTo("test.pdf");
        }
    }

    @Nested
    @DisplayName("Download Document Tests")
    class DownloadDocumentTests {

        @Test
        @DisplayName("Should download document successfully from MinIO")
        void shouldDownloadDocumentSuccessfully() throws Exception {
            // Arrange
            byte[] expectedContent = "test pdf content".getBytes();
            InputStream inputStream = new ByteArrayInputStream(expectedContent);

            when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
            when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(inputStream);

            // Act
            byte[] actualContent = documentService.downloadDocument(1L);

            // Assert
            assertThat(actualContent).isEqualTo(expectedContent);

            verify(documentRepository).findById(1L);
            verify(minioClient).getObject(any(GetObjectArgs.class));
        }

        @Test
        @DisplayName("Should throw DocumentNotFoundException when document not found")
        void shouldThrowExceptionWhenDocumentNotFound() {
            // Arrange
            when(documentRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> documentService.downloadDocument(999L))
                    .isInstanceOf(DocumentNotFoundException.class);

            verify(minioClient, never()).getObject(any(GetObjectArgs.class));
        }

        @Test
        @DisplayName("Should throw FileUploadException when MinIO download fails")
        void shouldThrowExceptionWhenMinioDownloadFails() throws Exception {
            // Arrange
            when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
            when(minioClient.getObject(any(GetObjectArgs.class)))
                    .thenThrow(new RuntimeException("MinIO error"));

            // Act & Assert
            assertThatThrownBy(() -> documentService.downloadDocument(1L))
                    .isInstanceOf(FileUploadException.class);
        }
    }

    @Nested
    @DisplayName("Get Document Tests")
    class GetDocumentTests {

        @Test
        @DisplayName("Should get document by ID successfully")
        void shouldGetDocumentByIdSuccessfully() {
            // Arrange
            when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));

            // Act
            DocumentDTO result = documentService.getDocumentById(1L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getFilename()).isEqualTo(testDocument.getFilename());
            assertThat(result.getOriginalFilename()).isEqualTo(testDocument.getOriginalFilename());
            assertThat(result.getInvoiceId()).isEqualTo(INVOICE_ID);

            verify(documentRepository).findById(1L);
        }

        @Test
        @DisplayName("Should throw DocumentNotFoundException when document not found")
        void shouldThrowExceptionWhenNotFound() {
            // Arrange
            when(documentRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> documentService.getDocumentById(999L))
                    .isInstanceOf(DocumentNotFoundException.class);
        }

        @Test
        @DisplayName("Should get documents by invoice ID successfully")
        void shouldGetDocumentsByInvoiceId() {
            // Arrange
            Document doc2 = Document.builder()
                    .id(2L)
                    .filename("file2.pdf")
                    .originalFilename("original2.pdf")
                    .contentType("application/pdf")
                    .fileSize(2048L)
                    .minioObjectName("file2.pdf")
                    .invoiceId(INVOICE_ID)
                    .uploadedBy(UPLOADED_BY)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(documentRepository.findByInvoiceId(INVOICE_ID))
                    .thenReturn(List.of(testDocument, doc2));

            // Act
            List<DocumentDTO> results = documentService.getDocumentsByInvoiceId(INVOICE_ID);

            // Assert
            assertThat(results).hasSize(2);
            assertThat(results).extracting(DocumentDTO::getId)
                    .containsExactlyInAnyOrder(1L, 2L);

            verify(documentRepository).findByInvoiceId(INVOICE_ID);
        }

        @Test
        @DisplayName("Should return empty list when no documents for invoice")
        void shouldReturnEmptyListWhenNoDocuments() {
            // Arrange
            when(documentRepository.findByInvoiceId(999L)).thenReturn(List.of());

            // Act
            List<DocumentDTO> results = documentService.getDocumentsByInvoiceId(999L);

            // Assert
            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("Delete Document Tests")
    class DeleteDocumentTests {

        @Test
        @DisplayName("Should delete document from MinIO and database")
        void shouldDeleteDocumentSuccessfully() throws Exception {
            // Arrange
            when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
            doNothing().when(minioClient).removeObject(any(RemoveObjectArgs.class));
            doNothing().when(documentRepository).delete(any(Document.class));

            // Act
            documentService.deleteDocument(1L);

            // Assert
            verify(documentRepository).findById(1L);
            verify(minioClient).removeObject(any(RemoveObjectArgs.class));
            verify(documentRepository).delete(testDocument);
        }

        @Test
        @DisplayName("Should throw DocumentNotFoundException when deleting non-existent document")
        void shouldThrowExceptionWhenDeletingNonExistent() {
            // Arrange
            when(documentRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> documentService.deleteDocument(999L))
                    .isInstanceOf(DocumentNotFoundException.class);

            verify(documentRepository, never()).delete(any(Document.class));
        }

        @Test
        @DisplayName("Should throw FileUploadException when MinIO delete fails")
        void shouldThrowExceptionWhenMinioDeleteFails() throws Exception {
            // Arrange
            when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
            doThrow(new RuntimeException("MinIO error"))
                    .when(minioClient).removeObject(any(RemoveObjectArgs.class));

            // Act & Assert
            assertThatThrownBy(() -> documentService.deleteDocument(1L))
                    .isInstanceOf(FileUploadException.class);

            verify(documentRepository, never()).delete(any(Document.class));
        }
    }

    @Nested
    @DisplayName("Utility Method Tests")
    class UtilityMethodTests {

        @Test
        @DisplayName("Should generate correct download URL")
        void shouldGenerateDownloadUrl() {
            // Act
            String url = documentService.generateDownloadUrl(123L);

            // Assert
            assertThat(url).isEqualTo("/api/documents/123/download");
        }
    }
}
