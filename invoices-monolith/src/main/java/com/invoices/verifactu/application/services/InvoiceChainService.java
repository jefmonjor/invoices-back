package com.invoices.verifactu.application.services;

import com.invoices.invoice.domain.entities.Company;
import com.invoices.invoice.domain.entities.Invoice;
import com.invoices.invoice.domain.entities.InvoiceStatus;
import com.invoices.invoice.domain.ports.CompanyRepository;
import com.invoices.invoice.domain.ports.InvoiceRepository;
import com.invoices.shared.domain.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Service for managing invoice hash chaining for Veri*Factu compliance.
 * Ensures sequential processing and maintains hash chain integrity per tenant.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceChainService {

    private final CompanyRepository companyRepository;
    private final InvoiceRepository invoiceRepository;

    /**
     * Calculates chained hash for an invoice using SHA-256.
     * 
     * @param invoice  The invoice to hash
     * @param lastHash Previous hash in the chain (from company)
     * @return New hash for this invoice
     */
    public String calculateChainedHash(Invoice invoice, String lastHash) {
        try {
            // Chain format: SHA256(invoice_number + total_amount + last_hash)
            // In production, use canonical XML representation as per Veri*Factu specs
            String dataToHash = invoice.getInvoiceNumber() +
                    (invoice.getTotalAmount() != null ? invoice.getTotalAmount().toString() : "0") +
                    (lastHash != null ? lastHash : "");

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(dataToHash.getBytes(StandardCharsets.UTF_8));
            String newHash = HexFormat.of().formatHex(encodedHash);

            log.debug("Calculated hash for invoice {}: {} (previous: {})",
                    invoice.getInvoiceNumber(), newHash, lastHash);

            return newHash;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    /**
     * Validates invoice before sending to AEAT.
     * Checks status, required fields, and business rules.
     * 
     * @param invoice Invoice to validate
     * @throws BusinessException if validation fails
     */
    public void validateInvoiceBeforeSending(Invoice invoice) {
        log.debug("Validating invoice {} before sending", invoice.getId());

        // Check status
        if (invoice.getStatus() != InvoiceStatus.PENDING &&
                invoice.getStatus() != InvoiceStatus.FINALIZED) {
            throw new BusinessException("INVALID_INVOICE_STATUS",
                    "Invoice must be PENDING or FINALIZED to send. Current: " + invoice.getStatus());
        }

        // Check required fields
        if (invoice.getInvoiceNumber() == null || invoice.getInvoiceNumber().isBlank()) {
            throw new BusinessException("MISSING_INVOICE_NUMBER", "Invoice number is required");
        }

        if (invoice.getTotalAmount() == null) {
            throw new BusinessException("MISSING_TOTAL_AMOUNT", "Total amount is required");
        }

        if (invoice.getCompanyId() == null) {
            throw new BusinessException("MISSING_COMPANY", "Company ID is required");
        }

        if (invoice.getClientId() == null) {
            throw new BusinessException("MISSING_CLIENT", "Client ID is required");
        }

        if (invoice.getItems() == null || invoice.getItems().isEmpty()) {
            throw new BusinessException("MISSING_ITEMS", "Invoice must have at least one item");
        }

        log.debug("Invoice {} validation passed", invoice.getId());
    }

    /**
     * Locks a company/tenant for update to ensure serial processing.
     * Uses pessimistic locking to prevent concurrent invoice submission.
     * 
     * @param companyId Company ID (tenant)
     * @return Locked company entity
     */
    @Transactional
    public Company lockTenantForUpdate(Long companyId) {
        log.debug("Acquiring lock for company {}", companyId);

        Company company = companyRepository.findByIdWithLock(companyId)
                .orElseThrow(() -> new BusinessException("COMPANY_NOT_FOUND",
                        "Company not found: " + companyId));

        log.debug("Lock acquired for company {}", companyId);
        return company;
    }

    /**
     * Updates the last hash for a company after successful invoice submission.
     * 
     * @param companyId Company ID
     * @param newHash   New hash to store
     * @return Updated company
     */
    @Transactional
    public Company updateTenantLastHash(Long companyId, String newHash) {
        log.debug("Updating last hash for company {} to: {}", companyId, newHash);

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException("COMPANY_NOT_FOUND",
                        "Company not found: " + companyId));

        Company updatedCompany = company.withLastHash(newHash);
        updatedCompany = companyRepository.save(updatedCompany);

        log.info("Updated last hash for company {}: {}", companyId, newHash);
        return updatedCompany;
    }

    /**
     * Prepares invoice for chaining by capturing previous hash and calculating new
     * one.
     * 
     * @param invoice Invoice to prepare
     * @param company Company (tenant) with lastHash
     * @return Updated invoice with hash fields set
     */
    public Invoice prepareInvoiceForChaining(Invoice invoice, Company company) {
        String lastHash = company.getLastHash();
        String newHash = calculateChainedHash(invoice, lastHash);

        invoice.setLastHashBefore(lastHash);
        invoice.setHash(newHash);

        log.info("Invoice {} prepared for chaining. Previous hash: {}, New hash: {}",
                invoice.getId(), lastHash, newHash);

        return invoice;
    }
}
