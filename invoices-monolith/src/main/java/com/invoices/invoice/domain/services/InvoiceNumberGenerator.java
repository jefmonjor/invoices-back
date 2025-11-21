package com.invoices.invoice.domain.services;

import com.invoices.invoice.domain.ports.InvoiceRepository;

import java.time.Year;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Domain service for generating unique, sequential invoice numbers.
 * Format: XXX/YYYY (e.g., 001/2025)
 * - XXX: Sequential number (001-999)
 * - YYYY: Current year
 */
public class InvoiceNumberGenerator {

    private final InvoiceRepository invoiceRepository;
    private static final Pattern NUMBER_PATTERN = Pattern.compile("^(\\d{3})/(\\d{4})$");

    public InvoiceNumberGenerator(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    /**
     * Generates the next invoice number for the current year.
     *
     * @return the generated invoice number (e.g., "001/2025")
     */
    public String generateNextNumber() {
        int currentYear = Year.now().getValue();
        Optional<String> lastNumberOpt = invoiceRepository.findLastInvoiceNumberByYear(currentYear);

        if (lastNumberOpt.isEmpty()) {
            return String.format("001/%d", currentYear);
        }

        String lastNumber = lastNumberOpt.get();
        Matcher matcher = NUMBER_PATTERN.matcher(lastNumber);

        if (matcher.matches()) {
            int sequence = Integer.parseInt(matcher.group(1));
            return String.format("%03d/%d", sequence + 1, currentYear);
        } else {
            // Fallback if last number doesn't match pattern (shouldn't happen with strict
            // validation)
            // Start fresh sequence for safety or log warning
            return String.format("001/%d", currentYear);
        }
    }
}
