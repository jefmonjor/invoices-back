package com.invoices.document.controller;

import com.invoices.document.dto.DocumentDTO;
import com.invoices.document.dto.UploadDocumentResponse;
import com.invoices.document.service.DocumentService;
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

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Document Service", description = "Document management API for uploading, downloading, and managing PDF files")
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a document", description = "Upload a PDF document and optionally associate it with an invoice")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Document uploaded successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UploadDocumentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid file type or file too large"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<UploadDocumentResponse> uploadDocument(
            @Parameter(description = "PDF file to upload", required = true)
            @RequestParam("file") MultipartFile file,

            @Parameter(description = "Invoice ID to associate with the document")
            @RequestParam(value = "invoiceId", required = false) Long invoiceId,

            @Parameter(description = "Username of the uploader")
            @RequestParam(value = "uploadedBy", required = false, defaultValue = "system") String uploadedBy
    ) {
        log.info("POST /api/documents - Uploading document: {}", file.getOriginalFilename());
        UploadDocumentResponse response = documentService.uploadDocument(file, invoiceId, uploadedBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get document metadata", description = "Retrieve metadata for a specific document by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Document metadata retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = DocumentDTO.class))),
            @ApiResponse(responseCode = "404", description = "Document not found")
    })
    public ResponseEntity<DocumentDTO> getDocumentById(
            @Parameter(description = "Document ID", required = true)
            @PathVariable Long id
    ) {
        log.info("GET /api/documents/{} - Fetching document metadata", id);
        DocumentDTO document = documentService.getDocumentById(id);
        return ResponseEntity.ok(document);
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download a document", description = "Download the PDF file for a specific document")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Document downloaded successfully",
                    content = @Content(mediaType = "application/pdf")),
            @ApiResponse(responseCode = "404", description = "Document not found"),
            @ApiResponse(responseCode = "500", description = "Error downloading document")
    })
    public ResponseEntity<byte[]> downloadDocument(
            @Parameter(description = "Document ID", required = true)
            @PathVariable Long id
    ) {
        log.info("GET /api/documents/{}/download - Downloading document", id);

        // Get metadata first to get original filename
        DocumentDTO metadata = documentService.getDocumentById(id);
        byte[] content = documentService.downloadDocument(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", metadata.getOriginalFilename());
        headers.setContentLength(content.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(content);
    }

    @GetMapping
    @Operation(summary = "Get documents by invoice ID", description = "Retrieve all documents associated with a specific invoice")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Documents retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = DocumentDTO.class)))
    })
    public ResponseEntity<List<DocumentDTO>> getDocumentsByInvoiceId(
            @Parameter(description = "Invoice ID", required = true)
            @RequestParam("invoiceId") Long invoiceId
    ) {
        log.info("GET /api/documents?invoiceId={} - Fetching documents for invoice", invoiceId);
        List<DocumentDTO> documents = documentService.getDocumentsByInvoiceId(invoiceId);
        return ResponseEntity.ok(documents);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a document", description = "Delete a document by ID from both storage and database")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Document deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Document not found"),
            @ApiResponse(responseCode = "500", description = "Error deleting document")
    })
    public ResponseEntity<Void> deleteDocument(
            @Parameter(description = "Document ID", required = true)
            @PathVariable Long id
    ) {
        log.info("DELETE /api/documents/{} - Deleting document", id);
        documentService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }
}
