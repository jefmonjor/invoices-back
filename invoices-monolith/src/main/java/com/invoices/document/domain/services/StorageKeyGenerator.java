package com.invoices.document.domain.services;

import java.time.LocalDateTime;

/**
 * Strategy interface for generating storage keys (paths) for documents.
 */
public interface StorageKeyGenerator {

    /**
     * Generates a storage key for an invoice PDF.
     * Format: invoices/{tenantId}/{yyyy}/{invoiceNumber}.pdf
     *
     * @param tenantId      The ID of the company/tenant
     * @param invoiceNumber The invoice number
     * @param date          The date of the invoice (used for year folder)
     * @return The full object key (path)
     */
    String generateInvoiceKey(Long tenantId, String invoiceNumber, LocalDateTime date);
}
