package com.invoices.invoice.presentation.controllers;

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
import org.springframework.http.HttpStatus;
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
public class InvoiceController {

    private final GetInvoiceByIdUseCase getInvoiceByIdUseCase;
    private final GetAllInvoicesUseCase getAllInvoicesUseCase;
    private final CreateInvoiceUseCase createInvoiceUseCase;
    private final UpdateInvoiceUseCase updateInvoiceUseCase;
    private final DeleteInvoiceUseCase deleteInvoiceUseCase;
    private final GenerateInvoicePdfUseCase generateInvoicePdfUseCase;
    private final InvoiceDtoMapper dtoMapper;

    public InvoiceController(
            GetInvoiceByIdUseCase getInvoiceByIdUseCase,
            GetAllInvoicesUseCase getAllInvoicesUseCase,
            CreateInvoiceUseCase createInvoiceUseCase,
            UpdateInvoiceUseCase updateInvoiceUseCase,
            DeleteInvoiceUseCase deleteInvoiceUseCase,
            GenerateInvoicePdfUseCase generateInvoicePdfUseCase,
            InvoiceDtoMapper dtoMapper) {
        this.getInvoiceByIdUseCase = getInvoiceByIdUseCase;
        this.getAllInvoicesUseCase = getAllInvoicesUseCase;
        this.createInvoiceUseCase = createInvoiceUseCase;
        this.updateInvoiceUseCase = updateInvoiceUseCase;
        this.deleteInvoiceUseCase = deleteInvoiceUseCase;
        this.generateInvoicePdfUseCase = generateInvoicePdfUseCase;
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
     * GET /invoices/{id}/pdf - Generate PDF for invoice
     */
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> generateInvoicePdf(@PathVariable Long id) {
        try {
            byte[] pdfContent = generateInvoicePdfUseCase.execute(id);

            return ResponseEntity.ok()
                    .header("Content-Type", "application/pdf")
                    .header("Content-Disposition", "inline; filename=invoice-" + id + ".pdf")
                    .body(pdfContent);
        } catch (InvoiceNotFoundException e) {
            // Return 404 for invoice not found
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            // Return 500 for data integrity issues (company/client not found)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        } catch (Exception e) {
            // Return 500 for any other errors
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
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
