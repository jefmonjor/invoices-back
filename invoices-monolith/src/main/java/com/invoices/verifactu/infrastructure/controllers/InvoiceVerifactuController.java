package com.invoices.verifactu.infrastructure.controllers;

import com.invoices.verifactu.domain.ports.VerifactuPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/verifactu/invoices")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Veri*Factu Invoices", description = "Operations for sending invoices to AEAT Veri*Factu system")
public class InvoiceVerifactuController {

    private final VerifactuPort verifactuPort;

    @PostMapping("/{invoiceId}/send")
    @PreAuthorize("hasRole('ADMIN') or @companySecurity.hasCompanyAccess(#companyId, 'ADMIN')")
    @Operation(summary = "Send invoice to Veri*Factu", description = "Manually triggers the sending of an invoice to AEAT Veri*Factu system")
    @ApiResponse(responseCode = "200", description = "Invoice sent successfully")
    @ApiResponse(responseCode = "400", description = "Invalid invoice status or data")
    @ApiResponse(responseCode = "404", description = "Invoice or Company not found")
    public ResponseEntity<Void> sendInvoice(
            @Parameter(description = "ID of the invoice to send") @PathVariable Long invoiceId,
            @Parameter(description = "ID of the company (tenant)") @RequestParam Long companyId) {

        log.info("Received request to send invoice {} for company {} to Veri*Factu", invoiceId, companyId);
        verifactuPort.sendInvoice(companyId, invoiceId);
        return ResponseEntity.ok().build();
    }
}
