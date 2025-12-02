package com.invoices.verifactu.application.services;

import com.invoices.invoice.domain.entities.Company;
import com.invoices.invoice.domain.ports.CompanyRepository;
import com.invoices.shared.domain.exception.BusinessException;
import com.invoices.shared.domain.ports.EncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Enumeration;

/**
 * Service for managing company certificates for Veri*Factu digital signatures.
 * Handles upload, validation, encryption, and retrieval of P12/PFX
 * certificates.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyCertificateService {

    private final CompanyRepository companyRepository;
    private final EncryptionService encryptionService;

    /**
     * Uploads and stores an encrypted certificate for a company
     * 
     * @param companyId Company ID (tenant)
     * @param file      PFX/P12 certificate file
     * @param password  Certificate password
     * @return Updated company with certificate reference
     */
    @Transactional
    public Company uploadCertificate(Long companyId, MultipartFile file, String password) {
        log.info("Uploading certificate for company {}", companyId);

        // 1. Validate file format
        validateP12Format(file);

        // 2. Load company
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException("COMPANY_NOT_FOUND", "Company not found: " + companyId));

        try {
            // 3. Read certificate file
            byte[] certificateBytes = file.getBytes();

            // 4. Validate certificate can be loaded with provided password
            KeyStore keyStore = loadKeyStore(certificateBytes, password);
            validateCertificate(keyStore);

            // 5. Encrypt certificate with AES (using application's encryption key)
            String encryptedCert = encryptionService.encrypt(Base64.getEncoder().encodeToString(certificateBytes));

            // 6. Encrypt password for storage
            String encryptedPassword = encryptionService.encrypt(password);

            // 7. Update company with encrypted certificate
            // Note: In production, consider storing cert in external KMS/Vault
            // For now, we store encrypted in DB with cert_ref pointing to local storage
            company = company.withCertificateData(encryptedCert, encryptedPassword);
            company = companyRepository.save(company);

            log.info("Certificate uploaded successfully for company {}", companyId);
            return company;

        } catch (Exception e) {
            log.error("Error uploading certificate for company {}", companyId, e);
            throw new BusinessException("CERTIFICATE_UPLOAD_FAILED", "Failed to upload certificate: " + e.getMessage());
        }
    }

    /**
     * Validates that the file is a valid P12/PFX format
     */
    public void validateP12Format(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("INVALID_CERTIFICATE", "Certificate file is empty");
        }

        String filename = file.getOriginalFilename();
        if (filename == null ||
                (!filename.toLowerCase().endsWith(".p12") && !filename.toLowerCase().endsWith(".pfx"))) {
            throw new BusinessException("INVALID_CERTIFICATE_FORMAT",
                    "Certificate must be in P12 or PFX format");
        }

        // Check file size (max 5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new BusinessException("CERTIFICATE_TOO_LARGE",
                    "Certificate file exceeds maximum size of 5MB");
        }
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class CertificateData {
        private KeyStore keyStore;
        private String password;
    }

    /**
     * Retrieves and decrypts certificate for signing operations
     * 
     * @param companyId Company ID
     * @return CertificateData containing KeyStore and password
     */
    public CertificateData getCertificateForSigning(Long companyId) {
        log.debug("Retrieving certificate for signing, company {}", companyId);

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException("COMPANY_NOT_FOUND", "Company not found: " + companyId));

        if (company.getCertRef() == null || company.getCertPassword() == null) {
            throw new BusinessException("CERTIFICATE_NOT_CONFIGURED",
                    "Certificate not configured for company: " + companyId);
        }

        try {
            // 1. Decrypt certificate
            String decryptedCertBase64 = encryptionService.decrypt(company.getCertRef());
            byte[] certificateBytes = Base64.getDecoder().decode(decryptedCertBase64);

            // 2. Decrypt password
            String decryptedPassword = encryptionService.decrypt(company.getCertPassword());

            // 3. Load KeyStore
            KeyStore keyStore = loadKeyStore(certificateBytes, decryptedPassword);
            return new CertificateData(keyStore, decryptedPassword);

        } catch (Exception e) {
            log.error("Error retrieving certificate for company {}", companyId, e);
            throw new BusinessException("CERTIFICATE_RETRIEVAL_FAILED",
                    "Failed to retrieve certificate: " + e.getMessage());
        }
    }

    /**
     * Loads a KeyStore from byte array
     */
    private KeyStore loadKeyStore(byte[] certificateBytes, String password) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(new ByteArrayInputStream(certificateBytes), password.toCharArray());
        return keyStore;
    }

    /**
     * Validates that the certificate is valid and not expired
     */
    private void validateCertificate(KeyStore keyStore) throws Exception {
        Enumeration<String> aliases = keyStore.aliases();

        if (!aliases.hasMoreElements()) {
            throw new BusinessException("INVALID_CERTIFICATE", "Certificate contains no valid aliases");
        }

        String alias = aliases.nextElement();
        X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);

        if (cert == null) {
            throw new BusinessException("INVALID_CERTIFICATE", "No certificate found in keystore");
        }

        // Check if certificate is expired
        try {
            cert.checkValidity();
        } catch (Exception e) {
            throw new BusinessException("CERTIFICATE_EXPIRED", "Certificate is expired or not yet valid");
        }

        log.info("Certificate validated: Subject={}, Valid until={}",
                cert.getSubjectX500Principal().getName(),
                cert.getNotAfter());
    }

    /**
     * Checks for certificates expiring within the specified days.
     * 
     * @param daysThreshold Number of days to check for expiration
     */
    @Transactional(readOnly = true)
    public void checkExpiringCertificates(int daysThreshold) {
        log.info("Checking for certificates expiring within {} days", daysThreshold);
        java.util.List<Company> companies = companyRepository.findAll();

        for (Company company : companies) {
            if (company.getCertRef() != null && company.getCertPassword() != null) {
                try {
                    CertificateData certData = getCertificateForSigning(company.getId());
                    KeyStore keyStore = certData.getKeyStore();
                    Enumeration<String> aliases = keyStore.aliases();
                    if (aliases.hasMoreElements()) {
                        String alias = aliases.nextElement();
                        X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);

                        java.util.Date expirationDate = cert.getNotAfter();
                        long daysUntilExpiration = java.time.temporal.ChronoUnit.DAYS.between(
                                java.time.Instant.now(),
                                expirationDate.toInstant());

                        if (daysUntilExpiration <= daysThreshold) {
                            if (daysUntilExpiration < 0) {
                                log.error("Certificate EXPIRED for company {}: {} (expired {} days ago)",
                                        company.getId(), company.getBusinessName(), Math.abs(daysUntilExpiration));
                                // TODO: Send alert email
                            } else {
                                log.warn("Certificate EXPIRING SOON for company {}: {} (expires in {} days)",
                                        company.getId(), company.getBusinessName(), daysUntilExpiration);
                                // TODO: Send warning email
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("Error checking certificate for company {}", company.getId(), e);
                }
            }
        }
    }
}
