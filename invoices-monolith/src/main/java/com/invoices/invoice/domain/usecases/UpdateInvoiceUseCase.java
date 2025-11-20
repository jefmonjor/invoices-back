package com.invoices.invoice.domain.usecases;

import com.invoices.invoice.domain.entities.Client;
import com.invoices.invoice.domain.entities.Invoice;
import com.invoices.invoice.domain.entities.InvoiceItem;
import com.invoices.invoice.domain.exceptions.InvoiceNotFoundException;
import com.invoices.invoice.domain.ports.ClientRepository;
import com.invoices.invoice.domain.ports.InvoiceEventPublisher;
import com.invoices.invoice.domain.ports.InvoiceRepository;
import com.invoices.invoice.domain.exceptions.ClientNotFoundException;

import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Use case: Update existing invoice.
 * Business logic for updating invoice details.
 *
 * IMMUTABLE FIELDS (cannot be changed after creation):
 * - companyId: Locked at creation
 * - invoiceNumber: Locked at creation
 *
 * UPDATABLE FIELDS:
 * - clientId: Can be changed (validates client exists)
 * - irpfPercentage: Can be changed (affects invoice calculations)
 * - rePercentage: Can be changed (affects invoice calculations)
 * - settlementNumber: Can be changed
 * - notes: Can be changed
 * - items: Can be replaced entirely
 */
public class UpdateInvoiceUseCase {

    private final InvoiceRepository invoiceRepository;
    private final ClientRepository clientRepository;
    private final InvoiceEventPublisher eventPublisher;

    public UpdateInvoiceUseCase(
            InvoiceRepository invoiceRepository,
            ClientRepository clientRepository,
            InvoiceEventPublisher eventPublisher) {
        this.invoiceRepository = invoiceRepository;
        this.clientRepository = clientRepository;
        this.eventPublisher = eventPublisher;
    }

    public Invoice execute(
            Long invoiceId,
            Long companyId,
            Long clientId,
            String invoiceNumber,
            String settlementNumber,
            BigDecimal irpfPercentage,
            BigDecimal rePercentage,
            List<InvoiceItem> updatedItems,
            String notes) {
        // Find existing invoice
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));

        // Validate truly immutable fields haven't changed (if provided)
        if (companyId != null && !companyId.equals(invoice.getCompanyId())) {
            throw new IllegalArgumentException(
                    "Cannot change company ID. Current: " + invoice.getCompanyId() + ", Requested: " + companyId);
        }

        if (invoiceNumber != null && !areInvoiceNumbersEquivalent(invoice.getInvoiceNumber(), invoiceNumber)) {
            throw new IllegalArgumentException(
                    "Cannot change invoice number. Current: " + invoice.getInvoiceNumber() + ", Requested: "
                            + invoiceNumber);
        }

        // Update client ID if provided and different
        if (clientId != null && !clientId.equals(invoice.getClientId())) {
            // Validate client exists
            clientRepository.findById(clientId)
                    .orElseThrow(() -> new ClientNotFoundException(clientId));
            invoice.setClientId(clientId);
        }

        // Update IRPF percentage if provided and different
        if (irpfPercentage != null && invoice.getIrpfPercentage().compareTo(irpfPercentage) != 0) {
            invoice.setIrpfPercentage(irpfPercentage);
        }

        // Update RE percentage if provided and different
        if (rePercentage != null && invoice.getRePercentage().compareTo(rePercentage) != 0) {
            invoice.setRePercentage(rePercentage);
        }

        // Update settlement number if provided
        if (settlementNumber != null) {
            invoice.setSettlementNumber(settlementNumber);
        }

        // Update items if provided
        if (updatedItems != null) {
            // Clear existing items using the proper domain method
            invoice.clearItems();

            // Add updated items
            updatedItems.forEach(invoice::addItem);
        }

        // Update notes if provided
        if (notes != null) {
            invoice.setNotes(notes);
        }

        // Save updated invoice
        Invoice updatedInvoice = invoiceRepository.save(invoice);

        // Get client email for event
        Client client = clientRepository.findById(invoice.getClientId())
                .orElseThrow(() -> new ClientNotFoundException(invoice.getClientId()));

        // Publish invoice updated event
        eventPublisher.publishInvoiceUpdated(updatedInvoice, client.getEmail());

        return updatedInvoice;
    }

    /**
     * Compares two invoice numbers for equivalence, normalizing optional letter prefixes.
     * This allows invoice numbers like "058/2025" and "A058/2025" to be considered equivalent
     * for validation purposes, since the prefix is optional and doesn't change the core number.
     *
     * Examples:
     * - "058/2025" and "A058/2025" are equivalent
     * - "047/2025" and "047/2025" are equivalent
     * - "A057/2025" and "B057/2025" are NOT equivalent (different prefixes)
     * - "058/2025" and "059/2025" are NOT equivalent (different numbers)
     *
     * @param existing the existing invoice number in the database
     * @param requested the requested invoice number from the update request
     * @return true if the invoice numbers are equivalent, false otherwise
     */
    private boolean areInvoiceNumbersEquivalent(String existing, String requested) {
        if (existing == null || requested == null) {
            return existing == null && requested == null;
        }

        // Direct match - most common case
        if (existing.equals(requested)) {
            return true;
        }

        // Normalize both numbers by extracting the numeric part with separators
        // Pattern matches: optional letter prefix + numeric part with separators (/, -, .)
        Pattern pattern = Pattern.compile("^([A-Za-z]*)(.*)$");

        Matcher existingMatcher = pattern.matcher(existing);
        Matcher requestedMatcher = pattern.matcher(requested);

        if (existingMatcher.matches() && requestedMatcher.matches()) {
            String existingPrefix = existingMatcher.group(1);
            String existingCore = existingMatcher.group(2);
            String requestedPrefix = requestedMatcher.group(1);
            String requestedCore = requestedMatcher.group(2);

            // If core numbers match, check if one has no prefix (allowing prefix to be added/removed)
            if (existingCore.equals(requestedCore)) {
                // Allow if one has no prefix and the other does (e.g., "058/2025" <-> "A058/2025")
                // or if both have the same prefix
                return existingPrefix.isEmpty() || requestedPrefix.isEmpty() || existingPrefix.equals(requestedPrefix);
            }
        }

        return false;
    }
}
