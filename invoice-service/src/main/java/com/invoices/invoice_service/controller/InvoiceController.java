package com.invoices.invoice_service.controller;

import com.invoices.invoice_service.dto.*;
import com.invoices.invoice_service.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para gestionar facturas
 */
@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Invoices", description = "API para gestionar facturas")
public class InvoiceController {

    private final InvoiceService invoiceService;

    /**
     * Crea una nueva factura
     * Endpoint: POST /api/invoices
     * Roles permitidos: USER, ADMIN
     */
    @PostMapping
    @Operation(summary = "Crear nueva factura", description = "Crea una nueva factura con sus items")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Factura creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<InvoiceDTO> createInvoice(
            @Valid @RequestBody @Parameter(description = "Datos de la factura a crear")
            CreateInvoiceRequest request) {
        log.info("Recibida solicitud para crear factura para cliente: {}", request.getClientId());
        InvoiceDTO invoiceDTO = invoiceService.createInvoice(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(invoiceDTO);
    }

    /**
     * Obtiene todas las facturas
     * Endpoint: GET /api/invoices
     * Query param opcional: clientId (filtrar por cliente)
     * Solo ADMIN puede ver todas las facturas
     */
    @GetMapping
    @Operation(summary = "Obtener todas las facturas",
            description = "Obtiene todas las facturas o las filtra por cliente si se proporciona clientId")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de facturas obtenida exitosamente"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<List<InvoiceDTO>> getAllInvoices(
            @RequestParam(required = false) @Parameter(description = "ID del cliente para filtrar")
            Long clientId) {
        log.info("Recibida solicitud para obtener facturas. ClientId: {}", clientId);

        List<InvoiceDTO> invoices;
        if (clientId != null) {
            invoices = invoiceService.getInvoicesByClientId(clientId);
        } else {
            invoices = invoiceService.getAllInvoices();
        }

        return ResponseEntity.ok(invoices);
    }

    /**
     * Obtiene una factura por ID
     * Endpoint: GET /api/invoices/{id}
     * Usuario solo puede ver sus propias facturas, ADMIN puede ver todas
     */
    @GetMapping("/{id}")
    @Operation(summary = "Obtener factura por ID", description = "Obtiene los detalles de una factura específica")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Factura encontrada"),
            @ApiResponse(responseCode = "404", description = "Factura no encontrada"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<InvoiceDTO> getInvoiceById(
            @PathVariable @Parameter(description = "ID de la factura") Long id) {
        log.info("Recibida solicitud para obtener factura con ID: {}", id);
        InvoiceDTO invoiceDTO = invoiceService.getInvoiceById(id);
        return ResponseEntity.ok(invoiceDTO);
    }

    /**
     * Actualiza una factura existente
     * Endpoint: PUT /api/invoices/{id}
     * Solo ADMIN o el dueño de la factura pueden actualizarla
     */
    @PutMapping("/{id}")
    @Operation(summary = "Actualizar factura", description = "Actualiza una factura existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Factura actualizada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Factura no encontrada"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<InvoiceDTO> updateInvoice(
            @PathVariable @Parameter(description = "ID de la factura") Long id,
            @Valid @RequestBody @Parameter(description = "Datos a actualizar") UpdateInvoiceRequest request) {
        log.info("Recibida solicitud para actualizar factura con ID: {}", id);
        InvoiceDTO invoiceDTO = invoiceService.updateInvoice(id, request);
        return ResponseEntity.ok(invoiceDTO);
    }

    /**
     * Elimina una factura
     * Endpoint: DELETE /api/invoices/{id}
     * Solo ADMIN puede eliminar facturas
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar factura", description = "Elimina una factura del sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Factura eliminada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Factura no encontrada"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<Void> deleteInvoice(
            @PathVariable @Parameter(description = "ID de la factura") Long id) {
        log.info("Recibida solicitud para eliminar factura con ID: {}", id);
        invoiceService.deleteInvoice(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Marca una factura como pagada
     * Endpoint: POST /api/invoices/{id}/pay
     */
    @PostMapping("/{id}/pay")
    @Operation(summary = "Marcar factura como pagada",
            description = "Cambia el estado de la factura a PAID y envía evento Kafka")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Factura marcada como pagada"),
            @ApiResponse(responseCode = "404", description = "Factura no encontrada"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<InvoiceDTO> markInvoiceAsPaid(
            @PathVariable @Parameter(description = "ID de la factura") Long id) {
        log.info("Recibida solicitud para marcar como pagada la factura con ID: {}", id);
        InvoiceDTO invoiceDTO = invoiceService.markAsPaid(id);
        return ResponseEntity.ok(invoiceDTO);
    }

    /**
     * Genera un PDF de la factura
     * Endpoint: POST /api/invoices/generate-pdf
     */
    @PostMapping("/generate-pdf")
    @Operation(summary = "Generar PDF de factura",
            description = "Genera un PDF de la factura y lo sube al document-service")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "PDF generado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Factura no encontrada"),
            @ApiResponse(responseCode = "500", description = "Error generando o subiendo el PDF")
    })
    public ResponseEntity<GeneratePdfResponse> generatePdf(
            @Valid @RequestBody @Parameter(description = "ID de la factura para generar PDF")
            GeneratePdfRequest request) {
        log.info("Recibida solicitud para generar PDF de factura con ID: {}", request.getInvoiceId());
        GeneratePdfResponse response = invoiceService.generatePdf(request.getInvoiceId());
        return ResponseEntity.ok(response);
    }
}
