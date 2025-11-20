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
import org.springframework.beans.factory.annotation.Value;
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
    private final GenerateInvoicePdfUseCase generateInvoicePdfUseCase;
    private final GetDocumentsByInvoiceUseCase getDocumentsByInvoiceUseCase;
    private final DownloadDocumentUseCase downloadDocumentUseCase;
    private final InvoiceDtoMapper dtoMapper;

    @Value("${invoice.pdf.enable-jasper-fallback:false}")
    private boolean enableJasperFallback;

    public InvoiceController(
            GetInvoiceByIdUseCase getInvoiceByIdUseCase,
            GetAllInvoicesUseCase getAllInvoicesUseCase,
            CreateInvoiceUseCase createInvoiceUseCase,
            UpdateInvoiceUseCase updateInvoiceUseCase,
            DeleteInvoiceUseCase deleteInvoiceUseCase,
            GenerateInvoicePdfUseCase generateInvoicePdfUseCase,
            GetDocumentsByInvoiceUseCase getDocumentsByInvoiceUseCase,
            DownloadDocumentUseCase downloadDocumentUseCase,
            InvoiceDtoMapper dtoMapper) {
        this.getInvoiceByIdUseCase = getInvoiceByIdUseCase;
        this.getAllInvoicesUseCase = getAllInvoicesUseCase;
        this.createInvoiceUseCase = createInvoiceUseCase;
        this.updateInvoiceUseCase = updateInvoiceUseCase;
        this.deleteInvoiceUseCase = deleteInvoiceUseCase;
        this.generateInvoicePdfUseCase = generateInvoicePdfUseCase;
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
                request.getInvoiceNumber(),
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
     * This endpoint retrieves the PDF stored in MinIO (generated by frontend).
     *
     * If no stored PDF is found:
     * - By default: Returns 404 with message indicating PDF must be generated from frontend
     * - If invoice.pdf.enable-jasper-fallback=true: Attempts to generate with JasperReports (legacy)
     */
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> getInvoicePdf(@PathVariable Long id) {
        try {
            log.info("GET /invoices/{}/pdf - Retrieving PDF for invoice", id);

            // Verify invoice exists first
            getInvoiceByIdUseCase.execute(id);

            // Try to find stored PDF documents for this invoice
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

            // No stored PDF found
            log.warn("No stored PDF found for invoice ID: {}. JasperReports fallback enabled: {}",
                    id, enableJasperFallback);

            if (enableJasperFallback) {
                // Optional fallback: Generate with JasperReports (legacy)
                log.info("Attempting to generate PDF with JasperReports for invoice ID: {}", id);
                try {
                    byte[] pdfContent = generateInvoicePdfUseCase.execute(id);
                    return ResponseEntity.ok()
                            .header("Content-Type", "application/pdf")
                            .header("Content-Disposition", "inline; filename=invoice-" + id + ".pdf")
                            .body(pdfContent);
                } catch (Exception jasperError) {
                    log.error("JasperReports fallback failed for invoice ID: {}", id, jasperError);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .header("Content-Type", "application/json")
                            .body(("{\"error\": \"PDF not found\", \"message\": \"No PDF document found for invoice " + id +
                                  " and JasperReports generation failed. Please generate the PDF from the frontend.\"}").getBytes());
                }
            }

            // Default: Return 404 with clear message
            log.info("Returning 404 - PDF must be generated from frontend for invoice ID: {}", id);
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
        } catch (IllegalStateException e) {
            log.error("Data integrity issue for invoice: {}", id, e);
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
}
