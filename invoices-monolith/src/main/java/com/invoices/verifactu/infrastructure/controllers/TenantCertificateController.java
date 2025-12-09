package com.invoices.verifactu.infrastructure.controllers;

import com.invoices.verifactu.application.services.CompanyCertificateService;
import com.invoices.verifactu.application.services.CompanyCertificateService.CertificateData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/verifactu/certificates")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Veri*Factu Certificates", description = "Management of digital certificates for Veri*Factu signing")
public class TenantCertificateController {

    private final CompanyCertificateService certificateService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN') or @companySecurity.hasCompanyAccess(#companyId, 'ADMIN')")
    @Operation(summary = "Upload company certificate", description = "Uploads a P12/PFX certificate for digital signing")
    @ApiResponse(responseCode = "200", description = "Certificate uploaded successfully")
    @ApiResponse(responseCode = "400", description = "Invalid file format or password")
    public ResponseEntity<Void> uploadCertificate(
            @Parameter(description = "Company ID (tenant)") @RequestParam Long companyId,
            @Parameter(description = "Certificate file (P12/PFX)") @RequestPart("file") MultipartFile file,
            @Parameter(description = "Certificate password") @RequestParam String password) {

        log.info("Received certificate upload request for company {}", companyId);
        certificateService.uploadCertificate(companyId, file, password);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN') or @companySecurity.hasCompanyAccess(#companyId, 'ADMIN')")
    @Operation(summary = "Get certificate status", description = "Returns the status and expiration date of the company's certificate")
    @ApiResponse(responseCode = "200", description = "Certificate status retrieved")
    @ApiResponse(responseCode = "404", description = "No certificate configured")
    public ResponseEntity<Map<String, Object>> getCertificateStatus(
            @Parameter(description = "Company ID (tenant)") @RequestParam Long companyId) {

        log.info("Getting certificate status for company {}", companyId);

        try {
            CertificateData certData = certificateService.getCertificateForSigning(companyId);
            KeyStore keyStore = certData.getKeyStore();

            Map<String, Object> status = new HashMap<>();
            status.put("configured", true);

            // Get certificate details
            Enumeration<String> aliases = keyStore.aliases();
            if (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);

                if (cert != null) {
                    Date notAfter = cert.getNotAfter();
                    Date notBefore = cert.getNotBefore();

                    status.put("subject", cert.getSubjectX500Principal().getName());
                    status.put("issuer", cert.getIssuerX500Principal().getName());
                    status.put("validFrom", LocalDateTime.ofInstant(notBefore.toInstant(), ZoneId.systemDefault()));
                    status.put("validUntil", LocalDateTime.ofInstant(notAfter.toInstant(), ZoneId.systemDefault()));
                    status.put("serialNumber", cert.getSerialNumber().toString());

                    // Check if expired or expiring soon
                    long daysUntilExpiration = java.time.temporal.ChronoUnit.DAYS.between(
                            java.time.Instant.now(), notAfter.toInstant());

                    status.put("daysUntilExpiration", daysUntilExpiration);

                    if (daysUntilExpiration < 0) {
                        status.put("status", "EXPIRED");
                    } else if (daysUntilExpiration <= 30) {
                        status.put("status", "EXPIRING_SOON");
                    } else {
                        status.put("status", "VALID");
                    }
                }
            }

            return ResponseEntity.ok(status);

        } catch (Exception e) {
            log.debug("No certificate configured for company {}: {}", companyId, e.getMessage());
            Map<String, Object> status = new HashMap<>();
            status.put("configured", false);
            status.put("status", "NOT_CONFIGURED");
            return ResponseEntity.ok(status);
        }
    }
}
