package com.invoices.invoice.presentation.controllers;

import com.invoices.document.domain.entities.Document;
import com.invoices.document.domain.usecases.DownloadDocumentUseCase;
import com.invoices.document.domain.usecases.GetDocumentsByInvoiceUseCase;
import com.invoices.invoice.domain.entities.Invoice;
import com.invoices.invoice.domain.entities.InvoiceItem;
import com.invoices.invoice.domain.exceptions.InvoiceNotFoundException;
import com.invoices.invoice.domain.usecases.*;
import com.invoices.invoice.dto.CreateInvoiceItemRequest;
import com.invoices.invoice.dto.CreateInvoiceRequest;
import com.invoices.invoice.dto.InvoiceDTO;
import com.invoices.invoice.dto.UpdateInvoiceRequest;
import com.invoices.invoice.presentation.mappers.InvoiceDtoMapper;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for invoice operations.
 * Presentation layer - delegates to use cases.
 * Implements complete CRUD operations.
 */
@RestController
@RequestMapping("/api/invoices")
@Slf4j
public class InvoiceController {

    private final GetInvoiceByIdUseCase getInvoiceByIdUseCase;
    private final GetAllInvoicesUseCase getAllInvoicesUseCase;
    private final CreateInvoiceUseCase createInvoiceUseCase;
    private final UpdateInvoiceUseCase updateInvoiceUseCase;
    private final DeleteInvoiceUseCase deleteInvoiceUseCase;
    private final GetDocumentsByInvoiceUseCase getDocumentsByInvoiceUseCase;
    private final DownloadDocumentUseCase downloadDocumentUseCase;
    private final InvoiceDtoMapper dtoMapper;

    public InvoiceController(
            GetInvoiceByIdUseCase getInvoiceByIdUseCase,
            GetAllInvoicesUseCase getAllInvoicesUseCase,
            CreateInvoiceUseCase createInvoiceUseCase,
            UpdateInvoiceUseCase updateInvoiceUseCase,
            DeleteInvoiceUseCase deleteInvoiceUseCase,
            GetDocumentsByInvoiceUseCase getDocumentsByInvoiceUseCase,
            DownloadDocumentUseCase downloadDocumentUseCase,
            InvoiceDtoMapper dtoMapper) {
        this.getInvoiceByIdUseCase = getInvoiceByIdUseCase;
        this.getAllInvoicesUseCase = getAllInvoicesUseCase;
        this.createInvoiceUseCase = createInvoiceUseCase;
        this.updateInvoiceUseCase = updateInvoiceUseCase;
        this.deleteInvoiceUseCase = deleteInvoiceUseCase;
        this.getDocumentsByInvoiceUseCase = getDocumentsByInvoiceUseCase;
        this.downloadDocumentUseCase = downloadDocumentUseCase;
        this.dtoMapper = dtoMapper;
    }

    /**
     * GET /invoices - Get all invoices
     */
    @GetMapping
    public ResponseEntity<List<InvoiceDTO>> getAllInvoices() {
        List<Invoice> invoices = getAllInvoicesUseCase.execute();
        List<InvoiceDTO> dtos = invoices.stream()
                .map(dtoMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * POST /invoices - Create new invoice
     */
    @PostMapping
    public ResponseEntity<InvoiceDTO> createInvoice(@Valid @RequestBody CreateInvoiceRequest request) {
        // Convert DTO items to domain InvoiceItems
        List<InvoiceItem> items = request.getItems().stream()
                .map(this::toDomainItem)
                .collect(Collectors.toList());

        // Execute use case
        Invoice invoice = createInvoiceUseCase.execute(
                request.getCompanyId(),
                request.getClientId(),
                request.getSettlementNumber(),
                request.getIrpfPercentage(),
                request.getRePercentage(),
                items,
                request.getNotes());

        // Convert to DTO and return
        InvoiceDTO dto = dtoMapper.toDto(invoice);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    /**
     * GET /invoices/{id} - Get invoice by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<InvoiceDTO> getInvoiceById(@PathVariable Long id) {
        Invoice invoice = getInvoiceByIdUseCase.execute(id);
        InvoiceDTO dto = dtoMapper.toDto(invoice);
        return ResponseEntity.ok(dto);
    }

    /**
     * PUT /invoices/{id} - Update invoice
     */
    @PutMapping("/{id}")
    public ResponseEntity<InvoiceDTO> updateInvoice(
            @PathVariable Long id,
            @Valid @RequestBody UpdateInvoiceRequest request) {
        // Convert DTO items to domain InvoiceItems
        List<InvoiceItem> updatedItems = null;
        if (request.getItems() != null) {
            updatedItems = request.getItems().stream()
                    .map(this::toDomainItem)
                    .collect(Collectors.toList());
        }

        // Execute use case
        Invoice invoice = updateInvoiceUseCase.execute(
                id,
                request.getCompanyId(),
                request.getClientId(),
                request.getInvoiceNumber(),
                request.getSettlementNumber(),
                request.getIrpfPercentage(),
                request.getRePercentage(),
                updatedItems,
                request.getNotes());

        // Convert to DTO and return
        InvoiceDTO dto = dtoMapper.toDto(invoice);
        return ResponseEntity.ok(dto);
    }

    /**
     * DELETE /invoices/{id} - Delete invoice
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInvoice(@PathVariable Long id) {
        deleteInvoiceUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /invoices/{id}/pdf - Get PDF for invoice
     *
     * Retrieves the PDF stored in MinIO (generated by frontend with React-PDF).
     * Returns 404 if no PDF document is found for the invoice.
     */
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> getInvoicePdf(@PathVariable Long id) {
        try {
            log.info("GET /invoices/{}/pdf - Retrieving PDF for invoice", id);

            // Verify invoice exists first
            getInvoiceByIdUseCase.execute(id);

            // Find stored PDF documents for this invoice
            List<Document> documents = getDocumentsByInvoiceUseCase.execute(id);

            if (!documents.isEmpty()) {
                // Get the first PDF document (assuming one PDF per invoice)
                Document pdfDocument = documents.stream()
                        .filter(doc -> doc.getOriginalFilename() != null &&
                                doc.getOriginalFilename().toLowerCase().endsWith(".pdf"))
                        .findFirst()
                        .orElse(null);

                if (pdfDocument != null) {
                    log.info("Found stored PDF document ID: {} for invoice ID: {}", pdfDocument.getId(), id);

                    // Download PDF from MinIO
                    try (InputStream pdfStream = downloadDocumentUseCase.execute(pdfDocument.getId())) {
                        byte[] pdfContent = pdfStream.readAllBytes();

                        log.info("Successfully retrieved stored PDF for invoice ID: {}, size: {} bytes",
                                id, pdfContent.length);

                        return ResponseEntity.ok()
                                .header("Content-Type", "application/pdf")
                                .header("Content-Disposition", "inline; filename=" + pdfDocument.getOriginalFilename())
                                .body(pdfContent);
                    }
                }
            }

            // No stored PDF found - return 404 with clear message
            log.warn("No PDF document found for invoice ID: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .header("Content-Type", "application/json")
                    .body(("{\"error\": \"PDF not found\", \"message\": \"No PDF document found for invoice " + id +
                            ". Please generate the PDF from the frontend first.\"}").getBytes());

        } catch (InvoiceNotFoundException e) {
            log.error("Invoice not found: {}", id);
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            log.error("Error reading PDF stream for invoice: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        } catch (Exception e) {
            log.error("Unexpected error retrieving PDF for invoice: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * Helper method: Convert CreateInvoiceItemRequest to domain InvoiceItem
     */
    private InvoiceItem toDomainItem(CreateInvoiceItemRequest itemRequest) {
        InvoiceItem item = new InvoiceItem(
                null, // ID will be generated
                null, // Invoice ID will be set by Invoice.addItem()
                itemRequest.getDescription(),
                itemRequest.getUnits(),
                itemRequest.getPrice(),
                itemRequest.getVatPercentage(),
                itemRequest.getDiscountPercentage() != null
                        ? itemRequest.getDiscountPercentage()
                        : BigDecimal.ZERO);

        // Set extended fields if present
        if (itemRequest.getItemDate() != null) {
            item.setItemDate(itemRequest.getItemDate());
        }
        if (itemRequest.getVehiclePlate() != null) {
            item.setVehiclePlate(itemRequest.getVehiclePlate());
        }
        if (itemRequest.getOrderNumber() != null) {
            item.setOrderNumber(itemRequest.getOrderNumber());
        }
        if (itemRequest.getZone() != null) {
            item.setZone(itemRequest.getZone());
        }
        if (itemRequest.getGasPercentage() != null) {
            item.setGasPercentage(itemRequest.getGasPercentage());
        }

        return item;
    }

    /**
     * GET /invoices/{id}/verification-status - Get VeriFactu verification status
     */
    @GetMapping("/{id}/verification-status")
    public ResponseEntity<com.invoices.invoice.dto.VerificationStatusDTO> getVerificationStatus(@PathVariable Long id) {
        log.info("Verification status requested for invoice ID: {}", id);

        try {
            Invoice invoice = getInvoiceByIdUseCase.execute(id);

            com.invoices.invoice.dto.VerificationStatusDTO status = com.invoices.invoice.dto.VerificationStatusDTO
                    .builder()
                    .status(invoice.getVerifactuStatus() != null ? invoice.getVerifactuStatus() : "NOT_SENT")
                    .txId(invoice.getVerifactuTxId())
                    .rawResponse(invoice.getVerifactuRawResponse())
                    .errorMessage(extractErrorMessage(invoice.getVerifactuRawResponse(), invoice.getVerifactuStatus()))
                    .estimatedTimeSeconds(calculateEstimatedTime(invoice.getVerifactuStatus()))
                    .build();

            return ResponseEntity.ok(status);
        } catch (InvoiceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private String extractErrorMessage(String rawResponse, String status) {
        if (!"REJECTED".equalsIgnoreCase(status) && !"FAILED".equalsIgnoreCase(status)) {
            return null;
        }

        if (rawResponse == null || rawResponse.isEmpty()) {
            return "Verification failed";
        }

        try {
            // Parse JSON to extract error message
            if (rawResponse.contains("descripcionError")) {
                int start = rawResponse.indexOf("descripcionError") + 19;
                int end = rawResponse.indexOf("\"", start);
                if (end > start) {
                    return rawResponse.substring(start, end);
                }
            }
        } catch (Exception e) {
            log.error("Error parsing raw response", e);
        }

        return "Verification rejected";
    }

    private Long calculateEstimatedTime(String status) {
        if ("PENDING".equalsIgnoreCase(status) || "PROCESSING".equalsIgnoreCase(status)) {
            return 5L; // 5 seconds estimated
        }
        return null;
    }
}
