package com.invoices.invoice.infrastructure.verifactu;

import com.invoices.invoice.domain.entities.Invoice;
import com.invoices.invoice.domain.enums.VerifactuStatus;
import com.invoices.invoice.domain.ports.InvoiceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Mock VeriFactu service with realistic delays and error rates.
 * 
 * Distribution:
 * - 70% ACCEPTED (2-3s delay)
 * - 10% REJECTED CIF inválido
 * - 10% REJECTED Formato XML
 * - 10% TIMEOUT (30s delay)
 */
@Service("verifactuMockService")
@RequiredArgsConstructor
@Slf4j
public class VerifactuMockService implements VerifactuServiceInterface {

    private final InvoiceRepository invoiceRepository;
    private final ObjectMapper objectMapper;
    private final Random random = new Random();

    @Override
    public String getServiceType() {
        return "MOCK";
    }

    @Override
    public void processInvoice(Long invoiceId) {
        log.info("[MOCK] Processing VeriFactu for invoice ID: {}", invoiceId);

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceId));

        try {
            // Determine outcome based on probability
            int probability = random.nextInt(100);

            if (probability < 70) {
                // 70% ACCEPTED with 2-3s delay
                processAccepted(invoice);
            } else if (probability < 80) {
                // 10% REJECTED - CIF inválido
                processRejectedInvalidCif(invoice);
            } else if (probability < 90) {
                // 10% REJECTED - Formato XML
                processRejectedXmlFormat(invoice);
            } else {
                // 10% TIMEOUT
                processTimeout(invoice);
            }

        } catch (Exception e) {
            log.error("[MOCK] Unexpected error processing invoice {}", invoiceId, e);
            invoice.setVerifactuStatus(VerifactuStatus.REJECTED.name());
            saveRawResponse(invoice, createErrorResponse("INTERNAL_ERROR", e.getMessage()));
            invoiceRepository.save(invoice);
        }
    }

    private void processAccepted(Invoice invoice) throws InterruptedException {
        // Simulate 2-3s processing delay
        int delayMs = 2000 + random.nextInt(1000);
        log.debug("[MOCK] Simulating ACCEPTED with {}ms delay", delayMs);
        TimeUnit.MILLISECONDS.sleep(delayMs);

        String txId = generateTxId();
        invoice.setVerifactuStatus(VerifactuStatus.ACCEPTED.name());
        invoice.setVerifactuTxId(txId);

        Map<String, Object> response = createSuccessResponse(txId, invoice);
        saveRawResponse(invoice, response);

        invoiceRepository.save(invoice);
        log.info("[MOCK] Invoice {} ACCEPTED with txId: {}", invoice.getInvoiceNumber(), txId);
    }

    private void processRejectedInvalidCif(Invoice invoice) {
        log.warn("[MOCK] Invoice {} REJECTED - CIF inválido", invoice.getInvoiceNumber());

        invoice.setVerifactuStatus(VerifactuStatus.REJECTED.name());
        invoice.setVerifactuTxId(null);

        Map<String, Object> response = createErrorResponse(
                "CIF_INVALIDO",
                "El CIF proporcionado no es válido o no está dado de alta en el censo de la AEAT");
        saveRawResponse(invoice, response);

        invoiceRepository.save(invoice);
    }

    private void processRejectedXmlFormat(Invoice invoice) {
        log.warn("[MOCK] Invoice {} REJECTED - Formato XML incorrecto", invoice.getInvoiceNumber());

        invoice.setVerifactuStatus(VerifactuStatus.REJECTED.name());
        invoice.setVerifactuTxId(null);

        Map<String, Object> response = createErrorResponse(
                "XML_MALFORMADO",
                "El formato del XML no cumple con el esquema SIF-GE. Verifique la estructura de datos");
        saveRawResponse(invoice, response);

        invoiceRepository.save(invoice);
    }

    private void processTimeout(Invoice invoice) throws InterruptedException {
        log.warn("[MOCK] Invoice {} TIMEOUT - Simulating 30s delay", invoice.getInvoiceNumber());

        // Simulate 30s timeout
        TimeUnit.SECONDS.sleep(30);

        invoice.setVerifactuStatus(VerifactuStatus.REJECTED.name());
        invoice.setVerifactuTxId(null);

        Map<String, Object> response = createErrorResponse(
                "TIMEOUT",
                "La AEAT no respondió en el tiempo esperado. Se reintentará automáticamente");
        saveRawResponse(invoice, response);

        invoiceRepository.save(invoice);
    }

    private String generateTxId() {
        String uuid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return String.format("MOCK-2025-%s", uuid);
    }

    private Map<String, Object> createSuccessResponse(String txId, Invoice invoice) {
        Map<String, Object> response = new HashMap<>();
        response.put("estado", "ACEPTADO");
        response.put("txId", txId);
        response.put("csv", "CSV-MOCK-" + UUID.randomUUID().toString().substring(0, 12));
        response.put("fechaHora", LocalDateTime.now().toString());
        response.put("numeroFactura", invoice.getInvoiceNumber());
        response.put("importeTotal", invoice.getTotalAmount());
        return response;
    }

    private Map<String, Object> createErrorResponse(String errorCode, String errorMessage) {
        Map<String, Object> response = new HashMap<>();
        response.put("estado", "RECHAZADO");
        response.put("codigoError", errorCode);
        response.put("descripcionError", errorMessage);
        response.put("fechaHora", LocalDateTime.now().toString());
        return response;
    }

    private void saveRawResponse(Invoice invoice, Map<String, Object> response) {
        try {
            String jsonResponse = objectMapper.writeValueAsString(response);
            invoice.setVerifactuRawResponse(jsonResponse);
        } catch (Exception e) {
            log.error("[MOCK] Error serializing response to JSON", e);
        }
    }
}
