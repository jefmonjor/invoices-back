package com.invoices.invoice_service.presentation.controllers;

import com.invoices.api.InvoicesApi;
import com.invoices.api.model.ErrorResponse;
import com.invoices.api.model.InvoiceConfigDTO;
import com.invoices.api.model.InvoiceDTO;
import com.invoices.invoice_service.domain.entities.Invoice;
import com.invoices.invoice_service.domain.exceptions.InvoiceNotFoundException;
import com.invoices.invoice_service.domain.usecases.GeneratePdfUseCase;
import com.invoices.invoice_service.domain.usecases.GetInvoiceByIdUseCase;
import com.invoices.invoice_service.presentation.mappers.InvoiceDtoMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for invoice operations.
 * Presentation layer - delegates to use cases.
 * Implements OpenAPI generated interface.
 */
@RestController
public class InvoiceController implements InvoicesApi {

    private final GetInvoiceByIdUseCase getInvoiceByIdUseCase;
    private final GeneratePdfUseCase generatePdfUseCase;
    private final InvoiceDtoMapper dtoMapper;

    public InvoiceController(
        GetInvoiceByIdUseCase getInvoiceByIdUseCase,
        GeneratePdfUseCase generatePdfUseCase,
        InvoiceDtoMapper dtoMapper
    ) {
        this.getInvoiceByIdUseCase = getInvoiceByIdUseCase;
        this.generatePdfUseCase = generatePdfUseCase;
        this.dtoMapper = dtoMapper;
    }

    @Override
    public ResponseEntity<InvoiceDTO> invoicesIdGet(Integer id) {
        try {
            Invoice invoice = getInvoiceByIdUseCase.execute(id.longValue());
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

    @Override
    public ResponseEntity<org.springframework.core.io.Resource> invoicesGeneratePdfPost(
        InvoiceConfigDTO invoiceConfigDTO
    ) {
        try {
            byte[] pdfBytes = generatePdfUseCase.execute(
                invoiceConfigDTO.getInvoiceNumber(),
                invoiceConfigDTO.getBaseAmount().doubleValue(),
                invoiceConfigDTO.getIrpfPercentage() != null
                    ? invoiceConfigDTO.getIrpfPercentage().doubleValue()
                    : null,
                invoiceConfigDTO.getRePercentage() != null
                    ? invoiceConfigDTO.getRePercentage().doubleValue()
                    : null,
                invoiceConfigDTO.getTotalAmount().doubleValue(),
                invoiceConfigDTO.getColor(),
                invoiceConfigDTO.getTextStyle()
            );

            org.springframework.core.io.Resource resource =
                new org.springframework.core.io.ByteArrayResource(pdfBytes);

            return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=invoice.pdf")
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .body(resource);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
