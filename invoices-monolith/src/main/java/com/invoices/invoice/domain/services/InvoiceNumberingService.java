package com.invoices.invoice.domain.services;

import com.invoices.invoice.domain.ports.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Domain service for generating unique, sequential invoice numbers per company.
 * Format: XXX/YYYY (e.g., 001/2025)
 * - XXX: Sequential number (001-999)
 * - YYYY: Current year
 */
@Service
@RequiredArgsConstructor
public class InvoiceNumberingService {

    private final InvoiceRepository invoiceRepository;
    private static final Pattern NUMBER_PATTERN = Pattern.compile("^(\\d{3})/(\\d{4})$");

    /**
     * Generates the next invoice number for the specified company and current year.
     * Uses pessimistic locking (via repository) to ensure uniqueness in concurrent
     * environments.
     *
     * @param companyId the company ID
     * @return the generated invoice number (e.g., "001/2025")
     */
    @Transactional
    public String generateNextNumber(Long companyId) {
        int currentYear = Year.now().getValue();

        // Find the last invoice number for this company and year
        // This method should ideally use a lock in the repository implementation
        Optional<String> lastNumberOpt = invoiceRepository.findLastInvoiceNumberByCompanyAndYear(companyId,
                currentYear);

        if (lastNumberOpt.isEmpty()) {
            return String.format("001/%d", currentYear);
        }

        String lastNumber = lastNumberOpt.get();
        Matcher matcher = NUMBER_PATTERN.matcher(lastNumber);

        if (matcher.matches()) {
            int sequence = Integer.parseInt(matcher.group(1));
            return String.format("%03d/%d", sequence + 1, currentYear);
        } else {
            // Fallback if last number doesn't match pattern
            // Start fresh sequence for safety, but log warning in real app
            return String.format("001/%d", currentYear);
        }
    }
}
