package com.invoices.document.controller;

import com.invoices.document.domain.entities.Document;
import com.invoices.document.domain.usecases.*;
import com.invoices.document.presentation.dto.DocumentDTO;
import com.invoices.document.presentation.dto.UploadDocumentResponse;
import com.invoices.document.presentation.mappers.DocumentDtoMapper;
import com.invoices.document.presentation.controllers.DocumentController;
import com.invoices.document.exception.DocumentNotFoundException;
import com.invoices.document.exception.FileUploadException;
import com.invoices.document.exception.InvalidFileTypeException;
import com.invoices.exception.GlobalExceptionHandler;
import com.invoices.security.infrastructure.interceptor.CompanySecurityInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for DocumentController.
 * Uses MockitoExtension and standaloneSetup for isolation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentController Unit Tests")
class DocumentControllerTest {

        private MockMvc mockMvc;

        @Mock
        private UploadDocumentUseCase uploadDocumentUseCase;

        @Mock
        private DownloadDocumentUseCase downloadDocumentUseCase;

        @Mock
        private GetDocumentByIdUseCase getDocumentByIdUseCase;

        @Mock
        private GetDocumentsByInvoiceUseCase getDocumentsByInvoiceUseCase;

        @Mock
        private DeleteDocumentUseCase deleteDocumentUseCase;

        @Mock
        private DocumentDtoMapper mapper;

        @Mock
        private CompanySecurityInterceptor companySecurityInterceptor;

        private DocumentController documentController;

        private DocumentDTO testDocumentDTO;
        private UploadDocumentResponse uploadResponse;
        private Document testDocument;
        private static final Long DOCUMENT_ID = 1L;
        private static final Long INVOICE_ID = 100L;
        private static final String UPLOADED_BY = "test-user";

        @BeforeEach
        void setUp() {
                // Manually create controller with mocked dependencies
                documentController = new DocumentController(
                                uploadDocumentUseCase,
                                downloadDocumentUseCase,
                                getDocumentByIdUseCase,
                                getDocumentsByInvoiceUseCase,
                                deleteDocumentUseCase,
                                mapper);

                mockMvc = MockMvcBuilders.standaloneSetup(documentController)
                                .setControllerAdvice(new GlobalExceptionHandler())
                                .addInterceptors(companySecurityInterceptor)
                                .build();

                testDocument = new Document(
                                DOCUMENT_ID,
                                "unique-file.pdf",
                                "test-document.pdf",
                                "application/pdf",
                                1024L,
                                "storage-unique-file.pdf",
                                INVOICE_ID,
                                UPLOADED_BY,
                                LocalDateTime.now());

                testDocumentDTO = DocumentDTO.builder()
                                .id(DOCUMENT_ID)
                                .filename("unique-file.pdf")
                                .originalFilename("test-document.pdf")
                                .contentType("application/pdf")
                                .fileSize(1024L)
                                .invoiceId(INVOICE_ID)
                                .uploadedBy(UPLOADED_BY)
                                .createdAt(LocalDateTime.now())
                                .downloadUrl("/api/documents/1/download")
                                .build();

                uploadResponse = UploadDocumentResponse.builder()
                                .documentId(DOCUMENT_ID)
                                .filename("test-document.pdf")
                                .downloadUrl("/api/documents/1/download")
                                .uploadedAt(LocalDateTime.now())
                                .build();

                try {
                        lenient().when(companySecurityInterceptor.preHandle(any(), any(), any())).thenReturn(true);
                } catch (Exception e) {
                        throw new RuntimeException(e);
                }
        }

        @Nested
        @DisplayName("POST /api/documents - Upload Document")
        class UploadDocumentTests {

                @Test
                @DisplayName("Should upload document successfully")
                void shouldUploadDocumentSuccessfully() throws Exception {
                        // Arrange
                        MockMultipartFile file = new MockMultipartFile(
                                        "file",
                                        "test.pdf",
                                        "application/pdf",
                                        "test content".getBytes());

                        when(uploadDocumentUseCase.execute(any(), eq("test.pdf"), eq(INVOICE_ID), eq(UPLOADED_BY)))
                                        .thenReturn(testDocument);
                        when(mapper.toUploadResponse(any(Document.class)))
                                        .thenReturn(uploadResponse);

                        // Act & Assert
                        mockMvc.perform(multipart("/api/documents")
                                        .file(file)
                                        .param("invoiceId", INVOICE_ID.toString())
                                        .param("uploadedBy", UPLOADED_BY))
                                        .andDo(print())
                                        .andExpect(status().isCreated())
                                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                        .andExpect(jsonPath("$.documentId", is(DOCUMENT_ID.intValue())))
                                        .andExpect(jsonPath("$.filename", is("test-document.pdf")))
                                        .andExpect(jsonPath("$.downloadUrl", notNullValue()));

                        verify(uploadDocumentUseCase).execute(any(), eq("test.pdf"), eq(INVOICE_ID), eq(UPLOADED_BY));
                }

                @Test
                @DisplayName("Should upload document without invoice ID")
                void shouldUploadDocumentWithoutInvoiceId() throws Exception {
                        // Arrange
                        MockMultipartFile file = new MockMultipartFile(
                                        "file",
                                        "test.pdf",
                                        "application/pdf",
                                        "test content".getBytes());

                        when(uploadDocumentUseCase.execute(any(), anyString(), eq(null), eq("system")))
                                        .thenReturn(testDocument);
                        when(mapper.toUploadResponse(any(Document.class)))
                                        .thenReturn(uploadResponse);

                        // Act & Assert
                        mockMvc.perform(multipart("/api/documents")
                                        .file(file))
                                        .andDo(print())
                                        .andExpect(status().isCreated());

                        verify(uploadDocumentUseCase).execute(any(), anyString(), eq(null), eq("system"));
                }

                @Test
                @DisplayName("Should return 400 for invalid file type")
                void shouldReturn400ForInvalidFileType() throws Exception {
                        // Arrange
                        MockMultipartFile file = new MockMultipartFile(
                                        "file",
                                        "test.txt",
                                        "text/plain",
                                        "test content".getBytes());

                        when(uploadDocumentUseCase.execute(any(), anyString(), any(), anyString()))
                                        .thenThrow(new InvalidFileTypeException("text/plain"));

                        // Act & Assert
                        mockMvc.perform(multipart("/api/documents")
                                        .file(file))
                                        .andDo(print())
                                        .andExpect(status().isBadRequest());
                }

                @Test
                @DisplayName("Should return 500 for file upload errors")
                void shouldReturn500ForFileUploadError() throws Exception {
                        // Arrange
                        MockMultipartFile file = new MockMultipartFile(
                                        "file",
                                        "test.pdf",
                                        "application/pdf",
                                        "test content".getBytes());

                        when(uploadDocumentUseCase.execute(any(), anyString(), any(), anyString()))
                                        .thenThrow(new FileUploadException("Upload failed"));

                        // Act & Assert
                        mockMvc.perform(multipart("/api/documents")
                                        .file(file))
                                        .andDo(print())
                                        .andExpect(status().isInternalServerError());
                }
        }

        @Nested
        @DisplayName("GET /api/documents/{id} - Get Document Metadata")
        class GetDocumentByIdTests {

                @Test
                @DisplayName("Should get document metadata successfully")
                void shouldGetDocumentMetadata() throws Exception {
                        // Arrange
                        when(getDocumentByIdUseCase.execute(DOCUMENT_ID)).thenReturn(testDocument);
                        when(mapper.toDTO(any(Document.class))).thenReturn(testDocumentDTO);

                        // Act & Assert
                        mockMvc.perform(get("/api/documents/{id}", DOCUMENT_ID))
                                        .andDo(print())
                                        .andExpect(status().isOk())
                                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                        .andExpect(jsonPath("$.id", is(DOCUMENT_ID.intValue())))
                                        .andExpect(jsonPath("$.originalFilename", is("test-document.pdf")))
                                        .andExpect(jsonPath("$.contentType", is("application/pdf")))
                                        .andExpect(jsonPath("$.invoiceId", is(INVOICE_ID.intValue())));

                        verify(getDocumentByIdUseCase).execute(DOCUMENT_ID);
                }

                @Test
                @DisplayName("Should return 404 when document not found")
                void shouldReturn404WhenNotFound() throws Exception {
                        // Arrange
                        when(getDocumentByIdUseCase.execute(999L))
                                        .thenThrow(new DocumentNotFoundException(999L));

                        // Act & Assert
                        mockMvc.perform(get("/api/documents/{id}", 999L))
                                        .andDo(print())
                                        .andExpect(status().isNotFound());

                        verify(getDocumentByIdUseCase).execute(999L);
                }
        }

        @Nested
        @DisplayName("GET /api/documents/{id}/download - Download Document")
        class DownloadDocumentTests {

                @Test
                @DisplayName("Should download document successfully")
                void shouldDownloadDocument() throws Exception {
                        // Arrange
                        byte[] content = "test pdf content".getBytes();
                        when(downloadDocumentUseCase.getDocumentMetadata(DOCUMENT_ID)).thenReturn(testDocument);
                        when(downloadDocumentUseCase.execute(DOCUMENT_ID))
                                        .thenReturn(new java.io.ByteArrayInputStream(content));

                        // Act & Assert
                        mockMvc.perform(get("/api/documents/{id}/download", DOCUMENT_ID))
                                        .andDo(print())
                                        .andExpect(status().isOk())
                                        .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                                        .andExpect(header().string("Content-Disposition",
                                                        containsString("attachment")))
                                        .andExpect(header().string("Content-Disposition",
                                                        containsString("test-document.pdf")))
                                        .andExpect(content().bytes(content));

                        verify(downloadDocumentUseCase).getDocumentMetadata(DOCUMENT_ID);
                        verify(downloadDocumentUseCase).execute(DOCUMENT_ID);
                }

                @Test
                @DisplayName("Should return 404 when downloading non-existent document")
                void shouldReturn404WhenDownloadingNonExistent() throws Exception {
                        // Arrange
                        when(downloadDocumentUseCase.getDocumentMetadata(999L))
                                        .thenThrow(new DocumentNotFoundException(999L));

                        // Act & Assert
                        mockMvc.perform(get("/api/documents/{id}/download", 999L))
                                        .andDo(print())
                                        .andExpect(status().isNotFound());

                        verify(downloadDocumentUseCase, never()).execute(any());
                }

                @Test
                @DisplayName("Should return 500 when download fails")
                void shouldReturn500WhenDownloadFails() throws Exception {
                        // Arrange
                        when(downloadDocumentUseCase.getDocumentMetadata(DOCUMENT_ID)).thenReturn(testDocument);
                        when(downloadDocumentUseCase.execute(DOCUMENT_ID))
                                        .thenThrow(new FileUploadException("Download failed"));

                        // Act & Assert
                        mockMvc.perform(get("/api/documents/{id}/download", DOCUMENT_ID))
                                        .andDo(print())
                                        .andExpect(status().isInternalServerError());
                }
        }

        @Nested
        @DisplayName("GET /api/documents?invoiceId={id} - Get Documents By Invoice")
        class GetDocumentsByInvoiceTests {

                @Test
                @DisplayName("Should get documents by invoice ID successfully")
                void shouldGetDocumentsByInvoiceId() throws Exception {
                        // Arrange
                        Document doc2 = new Document(
                                        2L,
                                        "unique-file-2.pdf",
                                        "document-2.pdf",
                                        "application/pdf",
                                        2048L,
                                        "storage-unique-file-2.pdf",
                                        INVOICE_ID,
                                        UPLOADED_BY,
                                        LocalDateTime.now());

                        DocumentDTO doc2DTO = DocumentDTO.builder()
                                        .id(2L)
                                        .filename("unique-file-2.pdf")
                                        .originalFilename("document-2.pdf")
                                        .contentType("application/pdf")
                                        .fileSize(2048L)
                                        .invoiceId(INVOICE_ID)
                                        .uploadedBy(UPLOADED_BY)
                                        .createdAt(LocalDateTime.now())
                                        .downloadUrl("/api/documents/2/download")
                                        .build();

                        when(getDocumentsByInvoiceUseCase.execute(INVOICE_ID))
                                        .thenReturn(List.of(testDocument, doc2));
                        when(mapper.toDTO(testDocument)).thenReturn(testDocumentDTO);
                        when(mapper.toDTO(doc2)).thenReturn(doc2DTO);

                        // Act & Assert
                        mockMvc.perform(get("/api/documents")
                                        .param("invoiceId", INVOICE_ID.toString()))
                                        .andDo(print())
                                        .andExpect(status().isOk())
                                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                        .andExpect(jsonPath("$", hasSize(2)))
                                        .andExpect(jsonPath("$[0].id", is(DOCUMENT_ID.intValue())))
                                        .andExpect(jsonPath("$[1].id", is(2)));

                        verify(getDocumentsByInvoiceUseCase).execute(INVOICE_ID);
                }

                @Test
                @DisplayName("Should return empty list when no documents for invoice")
                void shouldReturnEmptyListWhenNoDocuments() throws Exception {
                        // Arrange
                        when(getDocumentsByInvoiceUseCase.execute(999L)).thenReturn(List.of());

                        // Act & Assert
                        mockMvc.perform(get("/api/documents")
                                        .param("invoiceId", "999"))
                                        .andDo(print())
                                        .andExpect(status().isOk())
                                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                        .andExpect(jsonPath("$", hasSize(0)));
                }
        }

        @Nested
        @DisplayName("DELETE /api/documents/{id} - Delete Document")
        class DeleteDocumentTests {

                @Test
                @DisplayName("Should delete document successfully")
                void shouldDeleteDocument() throws Exception {
                        // Arrange
                        doNothing().when(deleteDocumentUseCase).execute(DOCUMENT_ID);

                        // Act & Assert
                        mockMvc.perform(delete("/api/documents/{id}", DOCUMENT_ID))
                                        .andDo(print())
                                        .andExpect(status().isNoContent());

                        verify(deleteDocumentUseCase).execute(DOCUMENT_ID);
                }

                @Test
                @DisplayName("Should return 404 when deleting non-existent document")
                void shouldReturn404WhenDeletingNonExistent() throws Exception {
                        // Arrange
                        doThrow(new DocumentNotFoundException(999L))
                                        .when(deleteDocumentUseCase).execute(999L);

                        // Act & Assert
                        mockMvc.perform(delete("/api/documents/{id}", 999L))
                                        .andDo(print())
                                        .andExpect(status().isNotFound());

                        verify(deleteDocumentUseCase).execute(999L);
                }

                @Test
                @DisplayName("Should return 500 when delete fails")
                void shouldReturn500WhenDeleteFails() throws Exception {
                        // Arrange
                        doThrow(new FileUploadException("Delete failed"))
                                        .when(deleteDocumentUseCase).execute(DOCUMENT_ID);

                        // Act & Assert
                        mockMvc.perform(delete("/api/documents/{id}", DOCUMENT_ID))
                                        .andDo(print())
                                        .andExpect(status().isInternalServerError());
                }
        }
}
