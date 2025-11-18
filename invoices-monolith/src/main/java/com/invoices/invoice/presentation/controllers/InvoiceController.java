package com.invoices.invoice.presentation.controllers;

import com.invoices.invoice.domain.entities.Invoice;
import com.invoices.invoice.domain.entities.InvoiceItem;
import com.invoices.invoice.domain.exceptions.InvalidInvoiceStateException;
import com.invoices.invoice.domain.exceptions.InvoiceNotFoundException;
import com.invoices.invoice.domain.usecases.*;
import com.invoices.invoice.dto.CreateInvoiceItemRequest;
import com.invoices.invoice.dto.CreateInvoiceRequest;
import com.invoices.invoice.dto.InvoiceDTO;
import com.invoices.invoice.dto.UpdateInvoiceRequest;
import com.invoices.invoice.exception.ClientNotFoundException;
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
@RequestMapping("/invoices")
public class InvoiceController {

    private final GetInvoiceByIdUseCase getInvoiceByIdUseCase;
    private final GetAllInvoicesUseCase getAllInvoicesUseCase;
    private final CreateInvoiceUseCase createInvoiceUseCase;
    private final UpdateInvoiceUseCase updateInvoiceUseCase;
    private final DeleteInvoiceUseCase deleteInvoiceUseCase;
    private final GeneratePdfUseCase generatePdfUseCase;
    private final InvoiceDtoMapper dtoMapper;

    public InvoiceController(
        GetInvoiceByIdUseCase getInvoiceByIdUseCase,
        GetAllInvoicesUseCase getAllInvoicesUseCase,
        CreateInvoiceUseCase createInvoiceUseCase,
        UpdateInvoiceUseCase updateInvoiceUseCase,
        DeleteInvoiceUseCase deleteInvoiceUseCase,
        GeneratePdfUseCase generatePdfUseCase,
        InvoiceDtoMapper dtoMapper
    ) {
        this.getInvoiceByIdUseCase = getInvoiceByIdUseCase;
        this.getAllInvoicesUseCase = getAllInvoicesUseCase;
        this.createInvoiceUseCase = createInvoiceUseCase;
        this.updateInvoiceUseCase = updateInvoiceUseCase;
        this.deleteInvoiceUseCase = deleteInvoiceUseCase;
        this.generatePdfUseCase = generatePdfUseCase;
        this.dtoMapper = dtoMapper;
    }

    /**
     * GET /invoices - Get all invoices
     */
    @GetMapping
    public ResponseEntity<List<InvoiceDTO>> getAllInvoices() {
        try {
            List<Invoice> invoices = getAllInvoicesUseCase.execute();
            List<InvoiceDTO> dtos = invoices.stream()
                .map(dtoMapper::toDto)
                .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * POST /invoices - Create new invoice
     */
    @PostMapping
    public ResponseEntity<InvoiceDTO> createInvoice(@Valid @RequestBody CreateInvoiceRequest request) {
        try {
            // Convert DTO items to domain InvoiceItems
            List<InvoiceItem> items = request.getItems().stream()
                .map(this::toDomainItem)
                .collect(Collectors.toList());

            // Execute use case
            Invoice invoice = createInvoiceUseCase.execute(
                request.getCompanyId(),
                request.getClientId(),
                request.getInvoiceNumber(),
                request.getIrpfPercentage(),
                request.getRePercentage(),
                items,
                request.getNotes()
            );

            // Convert to DTO and return
            InvoiceDTO dto = dtoMapper.toDto(invoice);
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);

        } catch (ClientNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /invoices/{id} - Get invoice by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<InvoiceDTO> getInvoiceById(@PathVariable Long id) {
        try {
            Invoice invoice = getInvoiceByIdUseCase.execute(id);
            InvoiceDTO dto = dtoMapper.toDto(invoice);
            return ResponseEntity.ok(dto);

        } catch (InvoiceNotFoundException e) {
            return ResponseEntity.notFound().build();

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * PUT /invoices/{id} - Update invoice
     */
    @PutMapping("/{id}")
    public ResponseEntity<InvoiceDTO> updateInvoice(
        @PathVariable Long id,
        @Valid @RequestBody UpdateInvoiceRequest request
    ) {
        try {
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
                updatedItems,
                request.getNotes()
            );

            // Convert to DTO and return
            InvoiceDTO dto = dtoMapper.toDto(invoice);
            return ResponseEntity.ok(dto);

        } catch (InvoiceNotFoundException e) {
            return ResponseEntity.notFound().build();

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * DELETE /invoices/{id} - Delete invoice
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInvoice(@PathVariable Long id) {
        try {
            deleteInvoiceUseCase.execute(id);
            return ResponseEntity.noContent().build();

        } catch (InvoiceNotFoundException e) {
            return ResponseEntity.notFound().build();

        } catch (InvalidInvoiceStateException e) {
            return ResponseEntity.badRequest().build();

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Helper method: Convert CreateInvoiceItemRequest to domain InvoiceItem
     */
    private InvoiceItem toDomainItem(CreateInvoiceItemRequest itemRequest) {
        return new InvoiceItem(
            null, // ID will be generated
            null, // Invoice ID will be set by Invoice.addItem()
            itemRequest.getDescription(),
            itemRequest.getUnits(),
            itemRequest.getPrice(),
            itemRequest.getVatPercentage(),
            itemRequest.getDiscountPercentage() != null
                ? itemRequest.getDiscountPercentage()
                : BigDecimal.ZERO
        );
    }
}
