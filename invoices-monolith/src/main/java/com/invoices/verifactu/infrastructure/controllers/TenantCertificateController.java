package com.invoices.verifactu.infrastructure.controllers;

import com.invoices.verifactu.application.services.CompanyCertificateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
}
