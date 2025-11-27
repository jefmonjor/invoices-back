package com.invoices.document.domain.services;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Implementation of StorageKeyGenerator for Backblaze B2 structure.
 * Enforces: invoices/{tenantId}/{yyyy}/{invoiceNumber}.pdf
 */
@Service
public class BackblazeStorageKeyGenerator implements StorageKeyGenerator {

    @Override
    public String generateInvoiceKey(Long tenantId, String invoiceNumber, LocalDateTime date) {
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID cannot be null");
        }
        if (invoiceNumber == null || invoiceNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Invoice number cannot be null or empty");
        }
        if (date == null) {
            date = LocalDateTime.now();
        }

        String year = String.valueOf(date.getYear());
        // Sanitize invoice number just in case (though it should be valid)
        String safeInvoiceNumber = invoiceNumber.replaceAll("[^a-zA-Z0-9\\-_]", "_");

        return String.format("invoices/%d/%s/%s.pdf", tenantId, year, safeInvoiceNumber);
    }
}
