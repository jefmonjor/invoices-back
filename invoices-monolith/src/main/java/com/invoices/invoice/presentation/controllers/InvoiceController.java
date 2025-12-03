package com.invoices.invoice.presentation.controllers;

import com.invoices.invoice.domain.entities.Client;
import com.invoices.invoice.domain.entities.Company;
import com.invoices.invoice.domain.entities.Invoice;
import com.invoices.invoice.domain.entities.InvoiceItem;
import com.invoices.invoice.domain.exceptions.InvoiceNotFoundException;
import com.invoices.invoice.domain.models.InvoiceSummary;
import com.invoices.invoice.domain.ports.ClientRepository;
import com.invoices.invoice.domain.ports.CompanyRepository;
import com.invoices.invoice.domain.ports.InvoiceRepository;
import com.invoices.invoice.domain.usecases.CreateInvoiceUseCase;
import com.invoices.invoice.domain.usecases.DeleteInvoiceUseCase;
import com.invoices.invoice.domain.usecases.GetAllInvoicesUseCase;
import com.invoices.invoice.domain.usecases.GetInvoiceByIdUseCase;
import com.invoices.invoice.domain.usecases.UpdateInvoiceUseCase;
import com.invoices.invoice.domain.ports.PdfGenerator;
import com.invoices.invoice.dto.CreateInvoiceItemRequest;
import com.invoices.invoice.dto.CreateInvoiceRequest;
import com.invoices.invoice.dto.InvoiceDTO;
import com.invoices.invoice.dto.UpdateInvoiceRequest;
import com.invoices.invoice.presentation.mappers.InvoiceDtoMapper;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
@org.springframework.security.access.prepost.PreAuthorize("!hasRole('PLATFORM_ADMIN')")
public class InvoiceController {

    private final GetInvoiceByIdUseCase getInvoiceByIdUseCase;
    private final GetAllInvoicesUseCase getAllInvoicesUseCase;
    private final CreateInvoiceUseCase createInvoiceUseCase;
    private final UpdateInvoiceUseCase updateInvoiceUseCase;
    private final DeleteInvoiceUseCase deleteInvoiceUseCase;
    private final InvoiceDtoMapper dtoMapper;
    private final InvoiceRepository invoiceRepository;
    private final CompanyRepository companyRepository;
    private final ClientRepository clientRepository;
    private final PdfGenerator pdfGenerator;

    public InvoiceController(
            GetInvoiceByIdUseCase getInvoiceByIdUseCase,
            GetAllInvoicesUseCase getAllInvoicesUseCase,
            CreateInvoiceUseCase createInvoiceUseCase,
            UpdateInvoiceUseCase updateInvoiceUseCase,
            DeleteInvoiceUseCase deleteInvoiceUseCase,
            InvoiceDtoMapper dtoMapper,
            InvoiceRepository invoiceRepository,
            CompanyRepository companyRepository,
            ClientRepository clientRepository,
            PdfGenerator pdfGenerator) {
        this.getInvoiceByIdUseCase = getInvoiceByIdUseCase;
        this.getAllInvoicesUseCase = getAllInvoicesUseCase;
        this.createInvoiceUseCase = createInvoiceUseCase;
        this.updateInvoiceUseCase = updateInvoiceUseCase;
        this.deleteInvoiceUseCase = deleteInvoiceUseCase;
        this.dtoMapper = dtoMapper;
        this.invoiceRepository = invoiceRepository;
        this.companyRepository = companyRepository;
        this.clientRepository = clientRepository;
        this.pdfGenerator = pdfGenerator;
    }

    /**
     * GET /invoices - Get all invoices (paginated)
     * Returns paginated list of invoices with X-Total-Count header for frontend
     * pagination
     */
    @GetMapping
    public ResponseEntity<List<InvoiceDTO>> getAllInvoices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status) {
        // checkPlatformAdminAccess(); // Handled by @PreAuthorize
        Long companyId = com.invoices.security.context.CompanyContext.getCompanyId();

        // Get total count for X-Total-Count header
        long totalCount = invoiceRepository.countByCompanyId(companyId, search, status);

        List<InvoiceSummary> invoices = getAllInvoicesUseCase.execute(companyId, page, size, search, status);
        List<InvoiceDTO> dtos = invoices.stream()
                .map(dtoMapper::toSummaryDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(totalCount))
                .body(dtos);
    }

    /**
     * POST /invoices - Create new invoice
     */
    @PostMapping
    public ResponseEntity<InvoiceDTO> createInvoice(@Valid @RequestBody CreateInvoiceRequest request) {
        // checkPlatformAdminAccess(); // Handled by @PreAuthorize
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
        // checkPlatformAdminAccess(); // Handled by @PreAuthorize
        Invoice invoice = getInvoiceByIdUseCase.execute(id);
        InvoiceDTO dto = dtoMapper.toDto(invoice);
        return ResponseEntity.ok(dto);
    }

    /**
     * PUT /invoices/{id} - Update invoice
     */
    @PutMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
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
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteInvoice(@PathVariable Long id) {
        deleteInvoiceUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /invoices/{id}/pdf - Download invoice PDF (draft or final)
     *
     * @param id      Invoice ID
     * @param version "draft" (default) or "final"
     * @return PDF bytes
     */
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(
            @PathVariable Long id,
            @RequestParam(defaultValue = "draft") String version) {

        log.info("GET /invoices/{}/pdf - version: {}", id, version);

        try {
            // Get invoice
            Invoice invoice = invoiceRepository.findById(id)
                    .orElseThrow(() -> new InvoiceNotFoundException(id));

            // Validation: only allow 'final' if ACCEPTED
            if ("final".equals(version)) {
                if (!"ACCEPTED".equals(invoice.getVerifactuStatus())) {
                    log.warn("PDF final requested for non-verified invoice: {}", id);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body("{\"error\": \"PDF final solo disponible para facturas verificadas\"}".getBytes());
                }
                if (invoice.getQrPayload() == null || invoice.getQrPayload().isEmpty()) {
                    log.warn("QR payload not available for invoice: {}", id);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body("{\"error\": \"QR payload no disponible\"}".getBytes());
                }
            }

            // Get Company and Client
            Company company = companyRepository.findById(invoice.getCompanyId())
                    .orElseThrow(() -> new RuntimeException("Company not found: " + invoice.getCompanyId()));
            Client client = clientRepository.findById(invoice.getClientId())
                    .orElseThrow(() -> new RuntimeException("Client not found: " + invoice.getClientId()));

            // Generate PDF (with or without QR depending on version)
            byte[] pdfBytes;
            if ("final".equals(version)) {
                log.info("Generating final PDF with QR for invoice: {}", id);
                pdfBytes = pdfGenerator.generateInvoicePdfWithQr(invoice, company, client, invoice.getQrPayload());
            } else {
                log.info("Generating draft PDF for invoice: {}", id);
                pdfBytes = pdfGenerator.generateInvoicePdf(invoice, company, client);
            }

            // Build filename
            String filename = String.format("Factura_%s_%s.pdf",
                    invoice.getInvoiceNumber().replace("/", "_"), version);

            log.info("Successfully generated PDF for invoice: {}, version: {}, size: {} bytes",
                    id, version, pdfBytes.length);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(pdfBytes);

        } catch (InvoiceNotFoundException e) {
            log.error("Invoice not found: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\": \"Invoice not found\"}".getBytes());
        } catch (Exception e) {
            log.error("Error generating PDF for invoice: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\": \"Error generating PDF\"}".getBytes());
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
     * GET /invoices/{id}/canonical - Get canonical JSON of invoice
     */
    @GetMapping("/{id}/canonical")
    public ResponseEntity<String> getCanonicalJson(@PathVariable Long id) {
        log.info("Canonical JSON requested for invoice ID: {}", id);

        try {
            Invoice invoice = getInvoiceByIdUseCase.execute(id);

            String canonicalJson = invoice.getDocumentJson();

            if (canonicalJson == null || canonicalJson.isEmpty()) {
                log.warn("No canonical JSON found for invoice ID: {}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .header("Content-Type", "application/json")
                        .body("{\"error\": \"Canonical JSON not found\", \"message\": \"No canonical JSON available for invoice "
                                + id + "\"}");
            }

            return ResponseEntity.ok()
                    .header("Content-Type", "application/json")
                    .body(canonicalJson);
        } catch (InvoiceNotFoundException e) {
            log.error("Invoice not found: {}", id);
            return ResponseEntity.notFound().build();
        }
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
                    .invoiceId(invoice.getId())
                    .invoiceNumber(invoice.getInvoiceNumber())
                    .verifactuStatus(invoice.getVerifactuStatus() != null ? invoice.getVerifactuStatus() : "NOT_SENT")
                    .verifactuTxId(invoice.getVerifactuTxId())
                    .documentHash(invoice.getDocumentHash())
                    .qrPayload(extractQrPayload(invoice))
                    .pdfIsFinal(invoice.getPdfIsFinal())
                    .createdAt(invoice.getCreatedAt())
                    .updatedAt(invoice.getUpdatedAt())
                    .rawResponse(invoice.getVerifactuRawResponse())
                    .errorMessage(extractErrorMessage(invoice.getVerifactuRawResponse(), invoice.getVerifactuStatus()))
                    .estimatedTimeSeconds(calculateEstimatedTime(invoice.getVerifactuStatus()))
                    .build();

            return ResponseEntity.ok(status);
        } catch (InvoiceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Extract QR payload from invoice raw response or return null if not available
     */
    private String extractQrPayload(Invoice invoice) {
        String rawResponse = invoice.getVerifactuRawResponse();
        if (rawResponse == null || rawResponse.isEmpty()) {
            return null;
        }

        try {
            // Try to parse JSON to extract qrPayload
            if (rawResponse.contains("qrPayload")) {
                int start = rawResponse.indexOf("qrPayload") + 12;
                int end = rawResponse.indexOf("\"", start);
                if (end > start) {
                    return rawResponse.substring(start, end);
                }
            }
        } catch (Exception e) {
            log.debug("Could not extract QR payload from raw response", e);
        }

        return null;
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

    // checkPlatformAdminAccess() removed - replaced by @PreAuthorize
}
