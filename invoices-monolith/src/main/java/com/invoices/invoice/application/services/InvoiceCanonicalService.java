package com.invoices.invoice.application.services;

import com.invoices.invoice.domain.entities.Invoice;
import com.invoices.invoice.domain.entities.InvoiceItem;
import com.invoices.invoice.domain.entities.Company;
import com.invoices.invoice.domain.entities.Client;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * Service responsible for generating the canonical representation of an invoice
 * and calculating its hash for VeriFactu compliance (chaining).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceCanonicalService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    /**
     * Calculates the SHA-256 hash of the canonical representation of the invoice.
     *
     * @param invoice      The invoice to hash
     * @param company      The issuing company
     * @param client       The recipient client
     * @param previousHash The hash of the previous invoice (for chaining)
     * @return The SHA-256 hash in hexadecimal format
     */
    public String calculateInvoiceHash(Invoice invoice, Company company, Client client, String previousHash) {
        String canonicalString = buildCanonicalString(invoice, company, client, previousHash);
        return sha256(canonicalString);
    }

    /**
     * Builds the canonical string representation of the invoice.
     * Format:
     * PREVIOUS_HASH|ISSUER_TAX_ID|INVOICE_NUMBER|ISSUE_DATE|TOTAL_AMOUNT|LINES_HASH
     */
    private String buildCanonicalString(Invoice invoice, Company company, Client client, String previousHash) {
        StringBuilder sb = new StringBuilder();

        // 1. Previous Hash (or empty if first)
        sb.append(previousHash != null ? previousHash : "").append("|");

        // 2. Issuer Tax ID
        sb.append(company.getTaxId()).append("|");

        // 3. Invoice Number
        sb.append(invoice.getInvoiceNumber()).append("|");

        // 4. Issue Date (YYYY-MM-DD)
        sb.append(invoice.getIssueDate().format(DATE_FORMATTER)).append("|");

        // 5. Total Amount (2 decimals)
        BigDecimal totalAmount = calculateTotalAmount(invoice);
        sb.append(totalAmount.setScale(2, RoundingMode.HALF_UP)).append("|");

        // 6. Lines Hash (to ensure integrity of content)
        String linesHash = calculateLinesHash(invoice);
        sb.append(linesHash);

        log.debug("Canonical string for invoice {}: {}", invoice.getInvoiceNumber(), sb.toString());
        return sb.toString();
    }

    /**
     * Calculates the total amount of the invoice.
     */
    private BigDecimal calculateTotalAmount(Invoice invoice) {
        return invoice.getItems().stream()
                .map(item -> {
                    BigDecimal subtotal = item.calculateSubtotal();
                    BigDecimal vatPercent = item.getVatPercentage() != null ? item.getVatPercentage() : BigDecimal.ZERO;
                    BigDecimal vatAmount = subtotal.multiply(vatPercent).divide(new BigDecimal("100"), 2,
                            RoundingMode.HALF_UP);
                    return subtotal.add(vatAmount);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculates a hash of all invoice lines sorted by description.
     */
    private String calculateLinesHash(Invoice invoice) {
        String linesCanonical = invoice.getItems().stream()
                .sorted(Comparator.comparing(InvoiceItem::getDescription))
                .map(item -> item.getDescription() + item.getUnits() + item.getPrice())
                .collect(Collectors.joining("|"));

        return sha256(linesCanonical);
    }

    /**
     * Computes SHA-256 hash of a string.
     */
    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    private String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
