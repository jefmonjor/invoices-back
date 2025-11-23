package com.invoices.invoice.infrastructure.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.invoices.invoice.domain.entities.Client;
import com.invoices.invoice.domain.entities.Company;
import com.invoices.invoice.domain.entities.Invoice;
import com.invoices.invoice.domain.entities.InvoiceItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Canonicalizer for invoices according to VERI*FACTU specification.
 *
 * Rules:
 * - Sort all keys alphabetically (recursive)
 * - Normalize monetary numbers to strings with 2 decimals: "1000.00"
 * - Normalize strings: trim + NFC unicode normalization
 * - Remove volatile fields: id, createdAt, updatedAt, internalId
 * - Serialize JSON without spaces
 * - UTF-8 encoding
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceCanonicalizer {

    private static final DateTimeFormatter ISO_8601_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private final ObjectMapper objectMapper;

    /**
     * Canonicalizes an invoice to produce a consistent JSON representation.
     *
     * @param invoice The invoice to canonicalize
     * @param company The issuing company
     * @param client  The recipient client
     * @return Canonical JSON string (no spaces, sorted keys)
     */
    public String canonicalize(Invoice invoice, Company company, Client client) {
        try {
            // Build canonical object with sorted keys (TreeMap maintains alphabetical
            // order)
            Map<String, Object> canonical = new TreeMap<>();

            // Add normalized fields in alphabetical order
            canonical.put("baseAmount", formatMoney(invoice.calculateBaseAmount()));

            // Client fields
            canonical.put("clientAddress", normalize(client.getAddress()));
            canonical.put("clientCity", normalize(client.getCity()));
            canonical.put("clientName", normalize(client.getBusinessName()));
            canonical.put("clientPostalCode", normalize(client.getPostalCode()));
            canonical.put("clientProvince", normalize(client.getProvince()));
            canonical.put("clientTaxId", normalize(client.getTaxId()));

            // Company fields
            canonical.put("companyAddress", normalize(company.getAddress()));
            canonical.put("companyCity", normalize(company.getCity()));
            canonical.put("companyName", normalize(company.getBusinessName()));
            canonical.put("companyPostalCode", normalize(company.getPostalCode()));
            canonical.put("companyProvince", normalize(company.getProvince()));
            canonical.put("companyTaxId", normalize(company.getTaxId()));

            // Invoice basic fields
            canonical.put("invoiceNumber", normalize(invoice.getInvoiceNumber()));
            canonical.put("irpfAmount", formatMoney(invoice.calculateIrpfAmount()));
            canonical.put("irpfPercentage", formatMoney(invoice.getIrpfPercentage()));
            canonical.put("issueDate",
                    invoice.getIssueDate().atZone(java.time.ZoneOffset.UTC).format(ISO_8601_FORMATTER));

            // Items (sorted by description to ensure consistency)
            List<Map<String, Object>> items = invoice.getItems().stream()
                    .sorted(Comparator.comparing(InvoiceItem::getDescription))
                    .map(this::canonicalizeItem)
                    .collect(Collectors.toList());
            canonical.put("items", items);

            // Optional fields (only if present)
            if (invoice.getNotes() != null && !invoice.getNotes().trim().isEmpty()) {
                canonical.put("notes", normalize(invoice.getNotes()));
            }

            canonical.put("reAmount", formatMoney(invoice.calculateReAmount()));
            canonical.put("rePercentage", formatMoney(invoice.getRePercentage()));

            if (invoice.getSettlementNumber() != null && !invoice.getSettlementNumber().trim().isEmpty()) {
                canonical.put("settlementNumber", normalize(invoice.getSettlementNumber()));
            }

            canonical.put("totalAmount", formatMoney(invoice.calculateTotalAmount()));

            // Serialize to JSON without spaces
            String canonicalJson = objectMapper.writeValueAsString(canonical);

            log.debug("Canonicalized invoice {}: {} bytes", invoice.getInvoiceNumber(), canonicalJson.length());
            return canonicalJson;

        } catch (Exception e) {
            log.error("Error canonicalizing invoice {}", invoice.getInvoiceNumber(), e);
            throw new RuntimeException("Failed to canonicalize invoice", e);
        }
    }

    /**
     * Canonicalizes an invoice item.
     */
    private Map<String, Object> canonicalizeItem(InvoiceItem item) {
        Map<String, Object> canonicalItem = new TreeMap<>();

        canonicalItem.put("description", normalize(item.getDescription()));

        // Discount percentage (default 0 if null)
        BigDecimal discount = item.getDiscountPercentage() != null ? item.getDiscountPercentage() : BigDecimal.ZERO;
        canonicalItem.put("discountPercentage", formatMoney(discount));

        canonicalItem.put("price", formatMoney(item.getPrice()));
        canonicalItem.put("quantity", formatMoney(BigDecimal.valueOf(item.getUnits())));
        canonicalItem.put("subtotal", formatMoney(item.calculateSubtotal()));
        canonicalItem.put("total", formatMoney(item.calculateTotal()));
        canonicalItem.put("vatPercentage", formatMoney(item.getVatPercentage()));

        return canonicalItem;
    }

    /**
     * Formats a monetary BigDecimal to string with exactly 2 decimals.
     * Example: 1000.00
     */
    private String formatMoney(BigDecimal amount) {
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /**
     * Normalizes a string:
     * - Trim whitespace
     * - NFC unicode normalization
     * - Return empty string if null
     */
    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        return Normalizer.normalize(trimmed, Normalizer.Form.NFC);
    }

    /**
     * Converts an invoice to compact JSON (non-canonical, for storage).
     * This is the original invoice representation, not the canonical one.
     */
    public String toCompactJson(Invoice invoice) {
        try {
            Map<String, Object> invoiceMap = new HashMap<>();

            // Basic fields
            invoiceMap.put("id", invoice.getId());
            invoiceMap.put("invoiceNumber", invoice.getInvoiceNumber());
            invoiceMap.put("companyId", invoice.getCompanyId());
            invoiceMap.put("clientId", invoice.getClientId());
            invoiceMap.put("issueDate", invoice.getIssueDate().toString());
            invoiceMap.put("status", invoice.getStatus().name());

            // Amounts
            invoiceMap.put("baseAmount", invoice.calculateBaseAmount().toString());
            invoiceMap.put("irpfPercentage", invoice.getIrpfPercentage().toString());
            invoiceMap.put("irpfAmount", invoice.calculateIrpfAmount().toString());
            invoiceMap.put("rePercentage", invoice.getRePercentage().toString());
            invoiceMap.put("reAmount", invoice.calculateReAmount().toString());
            invoiceMap.put("totalAmount", invoice.calculateTotalAmount().toString());

            // Optional fields
            if (invoice.getSettlementNumber() != null) {
                invoiceMap.put("settlementNumber", invoice.getSettlementNumber());
            }
            if (invoice.getNotes() != null) {
                invoiceMap.put("notes", invoice.getNotes());
            }

            // Items
            List<Map<String, Object>> itemsList = invoice.getItems().stream()
                    .map(item -> {
                        Map<String, Object> itemMap = new HashMap<>();
                        itemMap.put("description", item.getDescription());
                        itemMap.put("quantity", String.valueOf(item.getUnits()));
                        itemMap.put("price", item.getPrice().toString());
                        itemMap.put("vatPercentage", item.getVatPercentage().toString());
                        if (item.getDiscountPercentage() != null) {
                            itemMap.put("discountPercentage", item.getDiscountPercentage().toString());
                        }
                        return itemMap;
                    })
                    .collect(Collectors.toList());
            invoiceMap.put("items", itemsList);

            return objectMapper.writeValueAsString(invoiceMap);

        } catch (Exception e) {
            log.error("Error converting invoice to compact JSON", e);
            throw new RuntimeException("Failed to convert invoice to JSON", e);
        }
    }
}
