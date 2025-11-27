package com.invoices.invoice.presentation.controllers;

import com.invoices.invoice.infrastructure.verifactu.VerifactuServiceInterface;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@RestController
@RequestMapping("/webhooks/verifactu")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "VeriFactu Webhooks", description = "Callbacks from AEAT VeriFactu system")
public class VerifactuWebhookController {

    private final VerifactuServiceInterface verifactuService;

    @Value("${verifactu.webhook.secret:change-me-in-production}")
    private String webhookSecret;

    @PostMapping
    @Operation(summary = "Handle VeriFactu status update callback")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String payload,
            HttpServletRequest request) {

        String signature = request.getHeader("X-AEAT-Signature");
        String timestamp = request.getHeader("X-AEAT-Timestamp");

        if (signature == null || timestamp == null) {
            log.warn("Missing signature or timestamp in VeriFactu webhook");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Validate timestamp (prevent replay attacks, e.g., 5 minute window)
        try {
            long requestTime = Long.parseLong(timestamp);
            long currentTime = System.currentTimeMillis();
            if (Math.abs(currentTime - requestTime) > 300000) {
                log.warn("Invalid timestamp in VeriFactu webhook");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        } catch (NumberFormatException e) {
            log.warn("Malformed timestamp in VeriFactu webhook");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Validate signature
        if (!validateSignature(payload, timestamp, signature)) {
            log.warn("Invalid signature in VeriFactu webhook");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("Received valid VeriFactu webhook");

        // Process payload via service
        verifactuService.processWebhook(payload);

        return ResponseEntity.ok().build();
    }

    private boolean validateSignature(String payload, String timestamp, String signature) {
        try {
            String data = timestamp + "." + payload;
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKey);
            byte[] hash = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            String calculatedSignature = Base64.getEncoder().encodeToString(hash);
            return calculatedSignature.equals(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error validating signature", e);
            return false;
        }
    }
}
