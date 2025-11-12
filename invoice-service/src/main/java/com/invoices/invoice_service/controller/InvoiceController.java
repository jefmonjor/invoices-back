package com.invoices.invoice_service.controller;

import com.invoices.invoice_service.dto.ErrorResponse;
import com.invoices.invoice_service.dto.InvoiceConfigDTO;
import com.invoices.invoice_service.dto.InvoiceDTO;
import com.invoices.invoice_service.service.InvoiceService;
import com.invoices.invoice_service.service.PdfGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@Tag(name = "Invoices", description = "Invoice management and PDF generation")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final PdfGenerationService pdfGenerationService;

    @GetMapping("/{id}")
    @Operation(summary = "Get invoice by ID", description = "Retrieve invoice details along with its items for preview")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Invoice found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = InvoiceDTO.class))),
            @ApiResponse(responseCode = "404", description = "Invoice not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<?> getInvoiceById(@PathVariable Integer id) {
        try {
            InvoiceDTO invoice = invoiceService.getInvoiceById(id);
            return ResponseEntity.ok(invoice);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ErrorResponse.builder()
                            .code(404)
                            .message(e.getMessage())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorResponse.builder()
                            .code(500)
                            .message("Internal server error: " + e.getMessage())
                            .build());
        }
    }

    @PostMapping("/generate-pdf")
    @Operation(summary = "Generate PDF", description = "Generate a PDF based on provided invoice configuration")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "PDF generated successfully",
                    content = @Content(mediaType = "application/pdf")),
            @ApiResponse(responseCode = "400", description = "Invalid input",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<?> generatePdf(@RequestBody InvoiceConfigDTO configDTO) {
        try {
            // Crear la factura en la base de datos
            InvoiceDTO invoiceDTO = invoiceService.createInvoiceFromConfig(configDTO);

            // Generar el PDF
            byte[] pdfBytes = pdfGenerationService.generateInvoicePdf(invoiceDTO);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header("Content-Disposition", "attachment; filename=\"" + configDTO.getInvoiceNumber() + ".pdf\"")
                    .body(pdfBytes);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .code(400)
                            .message(e.getMessage())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorResponse.builder()
                            .code(500)
                            .message("Error generating PDF: " + e.getMessage())
                            .build());
        }
    }
}
