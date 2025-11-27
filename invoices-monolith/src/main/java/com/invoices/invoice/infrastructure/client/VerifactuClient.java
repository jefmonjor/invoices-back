package com.invoices.invoice.infrastructure.client;

import com.invoices.invoice.domain.entities.Invoice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Client for communicating with the VERI*FACTU government API.
 * Currently a mock implementation.
 */
@Component
@Slf4j
public class VerifactuClient {

    public String sendInvoice(Invoice invoice, String canonicalJson, String hash) {
        log.info("Sending invoice {} to VERI*FACTU", invoice.getInvoiceNumber());
        log.debug("Canonical JSON: {}", canonicalJson);
        log.debug("Hash: {}", hash);

        // Simulate network delay
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Simulate successful response
        String txId = UUID.randomUUID().toString();
        log.info("Invoice {} accepted by VERI*FACTU. TxID: {}", invoice.getInvoiceNumber(), txId);

        return txId;
    }
}
