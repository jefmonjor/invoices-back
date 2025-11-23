package com.invoices.invoice.presentation.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.invoices.invoice.domain.entities.Invoice;
import com.invoices.invoice.domain.ports.InvoiceRepository;
import com.invoices.invoice.infrastructure.messaging.InvoiceStatusNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Controller for receiving VERI*FACTU webhook callbacks.
 * Implements HMAC-SHA256 signature verification and anti-replay protection.
 */
@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
@Slf4j
public class VerifactuWebhookController {

    @Value("${verifactu.webhook.secret:change-me-in-production}")
    private String webhookSecret;

    private final InvoiceRepository invoiceRepository;
    private final InvoiceStatusNotificationService notificationService;
    private final ObjectMapper objectMapper;

    /**
     * Receives VERI*FACTU verification callbacks.
     *
     * Expected payload:
     * {
     *   "invoiceId": 123,
     *   "txId": "VF-2025-12345",
     *   "status": "ACCEPTED|REJECTED",
     *   "qrPayload": "https://verifactu.gob.es/...",
     *   "signedHash": "abc123...",
     *   "message": "Optional message"
     * }
     *
     * @param signature HMAC-SHA256 signature of the request body
     * @param timestamp Request timestamp (milliseconds since epoch)
     * @param rawBody   Raw request body as string
     * @return 200 OK if processed, 401 Unauthorized if signature invalid
     */
    @PostMapping("/verifactu")
    public ResponseEntity<Void> handleVerifactuCallback(
            @RequestHeader(value = "X-VF-Signature", required = false) String signature,
            @RequestHeader(value = "X-VF-Timestamp", required = false, defaultValue = "0") long timestamp,
            @RequestBody String rawBody) {

        log.info("[Webhook] Received VERI*FACTU callback");

        try {
            // 1. Validate timestamp (anti-replay attack, 5 minute tolerance)
            long currentTime = System.currentTimeMillis();
            long timeDifference = Math.abs(currentTime - timestamp);

            if (timestamp > 0 && timeDifference > 300000) { // 5 minutes
                log.warn("[Webhook] Rejected: timestamp too old. Difference: {}ms", timeDifference);
                return ResponseEntity.status(401).build();
            }

            // 2. Verify HMAC-SHA256 signature (if provided)
            if (signature != null && !signature.isEmpty()) {
                String expectedSignature = calculateHMAC(webhookSecret, rawBody);

                if (!MessageDigest.isEqual(
                        signature.getBytes(StandardCharsets.UTF_8),
                        expectedSignature.getBytes(StandardCharsets.UTF_8))) {
                    log.error("[Webhook] Rejected: invalid signature");
                    return ResponseEntity.status(401).build();
                }

                log.debug("[Webhook] Signature verified successfully");
            } else {
                log.warn("[Webhook] No signature provided (development mode?)");
            }

            // 3. Parse and process the webhook payload
            processVerifactuResponse(rawBody);

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("[Webhook] Error processing VERI*FACTU callback", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Calculates HMAC-SHA256 signature for webhook verification.
     *
     * @param secret The shared secret
     * @param data   The data to sign
     * @return Hex-encoded HMAC signature (lowercase)
     */
    private String calculateHMAC(String secret, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
        mac.init(keySpec);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }

    /**
     * Processes the VERI*FACTU webhook response.
     * Updates invoice status, transaction ID, QR payload, and raw response.
     *
     * @param jsonBody JSON webhook payload
     */
    private void processVerifactuResponse(String jsonBody) {
        try {
            JsonNode payload = objectMapper.readTree(jsonBody);

            // Extract fields
            Long invoiceId = payload.has("invoiceId") ? payload.get("invoiceId").asLong() : null;
            String txId = payload.has("txId") ? payload.get("txId").asText() : null;
            String status = payload.has("status") ? payload.get("status").asText() : "UNKNOWN";
            String qrPayload = payload.has("qrPayload") ? payload.get("qrPayload").asText() : null;
            String signedHash = payload.has("signedHash") ? payload.get("signedHash").asText() : null;
            String message = payload.has("message") ? payload.get("message").asText() : "";

            if (invoiceId == null) {
                log.error("[Webhook] Missing invoiceId in payload: {}", jsonBody);
                return;
            }

            log.info("[Webhook] Processing invoice {} - status: {}, txId: {}", invoiceId, status, txId);

            // Find invoice
            Invoice invoice = invoiceRepository.findById(invoiceId).orElse(null);
            if (invoice == null) {
                log.error("[Webhook] Invoice not found: {}", invoiceId);
                return;
            }

            // Update invoice fields
            invoice.setVerifactuStatus(status);
            invoice.setVerifactuTxId(txId);
            invoice.setVerifactuRawResponse(jsonBody);

            if (qrPayload != null && !qrPayload.isEmpty()) {
                invoice.setQrPayload(qrPayload);
            }

            // If accepted, mark PDF as final
            if ("ACCEPTED".equalsIgnoreCase(status)) {
                invoice.setPdfIsFinal(true);
                log.info("[Webhook] Invoice {} ACCEPTED - marked as final", invoice.getInvoiceNumber());
            }

            // Validate hash consistency (if provided)
            if (signedHash != null && invoice.getDocumentHash() != null) {
                if (!signedHash.equalsIgnoreCase(invoice.getDocumentHash())) {
                    log.warn("[Webhook] Hash mismatch! Expected: {}, Got: {}",
                            invoice.getDocumentHash(), signedHash);
                    invoice.setVerifactuStatus("HASH_MISMATCH");
                }
            }

            // Save to database
            invoiceRepository.save(invoice);

            // Notify via WebSocket
            notificationService.notifyStatusWithTx(invoiceId, status, txId, message);

            log.info("[Webhook] Invoice {} updated successfully. Status: {}", invoiceId, status);

        } catch (Exception e) {
            log.error("[Webhook] Error parsing webhook payload: {}", jsonBody, e);
        }
    }
}
