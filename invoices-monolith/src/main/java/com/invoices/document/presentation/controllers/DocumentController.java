package com.invoices.document.presentation.controllers;

import com.invoices.document.domain.entities.Document;
import com.invoices.document.domain.entities.FileContent;
import com.invoices.document.domain.usecases.*;
import com.invoices.document.presentation.dto.DocumentDTO;
import com.invoices.document.presentation.dto.UploadDocumentResponse;
import com.invoices.document.presentation.mappers.DocumentDtoMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for document management operations (Clean Architecture).
 * Uses Use Cases from domain layer instead of service layer.
 */
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Document Service", description = "Document management API for uploading, downloading, and managing PDF files")
public class DocumentController {

        private final UploadDocumentUseCase uploadDocumentUseCase;
        private final DownloadDocumentUseCase downloadDocumentUseCase;
        private final GetDocumentByIdUseCase getDocumentByIdUseCase;
        private final GetDocumentsByInvoiceUseCase getDocumentsByInvoiceUseCase;
        private final DeleteDocumentUseCase deleteDocumentUseCase;
        private final DocumentDtoMapper mapper;

        @PostMapping
        @Operation(summary = "Upload a document", description = "Upload a PDF document and optionally associate it with an invoice. Accepts multipart/form-data")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "Document uploaded successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UploadDocumentResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid file type or file too large"),
                        @ApiResponse(responseCode = "405", description = "Wrong Content-Type - must be multipart/form-data"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<UploadDocumentResponse> uploadDocument(
                        @Parameter(description = "PDF file to upload", required = true) @RequestParam("file") MultipartFile file,

                        @Parameter(description = "Invoice ID to associate with the document") @RequestParam(value = "invoiceId", required = false) Long invoiceId,

                        @Parameter(description = "Username of the uploader") @RequestParam(value = "uploadedBy", required = false, defaultValue = "system") String uploadedBy) {
                log.info("POST /api/documents - Uploading document: {}, size: {} bytes, contentType: {}",
                                file.getOriginalFilename(), file.getSize(), file.getContentType());

                // Validate file is not empty
                if (file.isEmpty()) {
                        log.error("Upload failed: File is empty");
                        throw new IllegalArgumentException("File cannot be empty");
                }
                // Convert MultipartFile to domain FileContent
                FileContent fileContent = new FileContent(
                                file::getInputStream,
                                file.getSize(),
                                file.getContentType());

                // Execute use case
                Document uploadedDocument = uploadDocumentUseCase.execute(
                                fileContent,
                                file.getOriginalFilename(),
                                invoiceId,
                                uploadedBy);

                // Map to response DTO
                UploadDocumentResponse response = mapper.toUploadResponse(uploadedDocument);

                log.info("Document uploaded successfully with ID: {}", uploadedDocument.getId());
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        @GetMapping("/{id}")
        @Operation(summary = "Get document metadata", description = "Retrieve metadata for a specific document by ID")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Document metadata retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = DocumentDTO.class))),
                        @ApiResponse(responseCode = "404", description = "Document not found")
        })
        public ResponseEntity<DocumentDTO> getDocumentById(
                        @Parameter(description = "Document ID", required = true) @PathVariable Long id) {
                log.info("GET /api/documents/{} - Fetching document metadata", id);

                Document document = getDocumentByIdUseCase.execute(id);
                DocumentDTO documentDTO = mapper.toDTO(document);

                log.info("Document metadata retrieved successfully: {}", id);
                return ResponseEntity.ok(documentDTO);
        }

        @GetMapping("/{id}/download")
        @Operation(summary = "Download a document", description = "Download the PDF file for a specific document")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Document downloaded successfully", content = @Content(mediaType = "application/pdf")),
                        @ApiResponse(responseCode = "404", description = "Document not found"),
                        @ApiResponse(responseCode = "500", description = "Error downloading document")
        })
        public ResponseEntity<byte[]> downloadDocument(
                        @Parameter(description = "Document ID", required = true) @PathVariable Long id) {
                log.info("GET /api/documents/{}/download - Downloading document", id);

                // Get metadata first to get original filename
                Document metadata = downloadDocumentUseCase.getDocumentMetadata(id);

                // Get file content
                try (InputStream inputStream = downloadDocumentUseCase.execute(id)) {
                        byte[] content = inputStream.readAllBytes();

                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.APPLICATION_PDF);
                        headers.setContentDispositionFormData("attachment", metadata.getOriginalFilename());
                        headers.setContentLength(content.length);

                        log.info("Document downloaded successfully: {}", id);
                        return ResponseEntity.ok()
                                        .headers(headers)
                                        .body(content);

                } catch (IOException e) {
                        log.error("Failed to read downloaded file stream", e);
                        throw new IllegalArgumentException("Failed to read downloaded file: " + e.getMessage());
                }
        }

        @GetMapping
        @Operation(summary = "Get documents by invoice ID", description = "Retrieve all documents associated with a specific invoice")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Documents retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = DocumentDTO.class)))
        })
        public ResponseEntity<List<DocumentDTO>> getDocumentsByInvoiceId(
                        @Parameter(description = "Invoice ID", required = true) @RequestParam("invoiceId") Long invoiceId) {
                log.info("GET /api/documents?invoiceId={} - Fetching documents for invoice", invoiceId);

                List<Document> documents = getDocumentsByInvoiceUseCase.execute(invoiceId);
                List<DocumentDTO> documentDTOs = documents.stream()
                                .map(mapper::toDTO)
                                .collect(Collectors.toList());

                log.info("Retrieved {} documents for invoice {}", documentDTOs.size(), invoiceId);
                return ResponseEntity.ok(documentDTOs);
        }

        @DeleteMapping("/{id}")
        @Operation(summary = "Delete a document", description = "Delete a document by ID from both storage and database")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "204", description = "Document deleted successfully"),
                        @ApiResponse(responseCode = "404", description = "Document not found"),
                        @ApiResponse(responseCode = "500", description = "Error deleting document")
        })
        public ResponseEntity<Void> deleteDocument(
                        @Parameter(description = "Document ID", required = true) @PathVariable Long id) {
                log.info("DELETE /api/documents/{} - Deleting document", id);

                deleteDocumentUseCase.execute(id);

                log.info("Document deleted successfully: {}", id);
                return ResponseEntity.noContent().build();
        }
}
