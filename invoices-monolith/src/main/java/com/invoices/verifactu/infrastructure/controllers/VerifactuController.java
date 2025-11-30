package com.invoices.verifactu.infrastructure.controllers;

import com.invoices.verifactu.domain.ports.VerifactuPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/verifactu")
@RequiredArgsConstructor
@Slf4j
public class VerifactuController {

    private final VerifactuPort verifactuPort;

    @PostMapping("/invoices/{invoiceId}/send")
    public ResponseEntity<Void> sendInvoice(
            @PathVariable Long invoiceId,
            @RequestParam Long companyId) {
        log.info("Received request to send invoice {} for company {} to Veri*Factu", invoiceId, companyId);
        verifactuPort.sendInvoice(companyId, invoiceId);
        return ResponseEntity.ok().build();
    }
}
