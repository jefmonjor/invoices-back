package com.invoices.verifactu.infrastructure.jobs;

import com.invoices.invoice.domain.entities.Invoice;
import com.invoices.invoice.domain.ports.InvoiceRepository;
import com.invoices.invoice.infrastructure.services.SmtpEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Scheduled job to retry pending VeriFactu submissions.
 * Runs every 5 minutes to ensure no invoice is lost when AEAT fails.
 * 
 * Each company's invoices are processed independently.
 * Max retries: 5 attempts before marking as FAILED and sending email
 * notification.
 * 
 * This job marks PENDING invoices as PROCESSING so the VeriFactu batch
 * scheduler can pick them up. If an invoice has been PENDING/PROCESSING
 * for too long, it increments the retry count.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VerifactuRetryJob {

    private static final int MAX_RETRY_ATTEMPTS = 5;
    private static final List<String> RETRYABLE_STATUSES = List.of("PENDING", "PROCESSING");

    private final InvoiceRepository invoiceRepository;
    private final SmtpEmailService emailService;

    /**
     * Runs every 5 minutes to retry pending VeriFactu submissions.
     * Cron: second minute hour day month weekday
     */
    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void retryPendingSubmissions() {
        log.info("Starting VeriFactu retry job...");

        try {
            List<Invoice> pendingInvoices = invoiceRepository.findByVerifactuStatusIn(RETRYABLE_STATUSES);

            if (pendingInvoices.isEmpty()) {
                log.debug("No pending invoices to retry");
                return;
            }

            log.info("Found {} pending invoices to check", pendingInvoices.size());

            int retryCount = 0;
            int failedCount = 0;
            int skippedCount = 0;

            for (Invoice invoice : pendingInvoices) {
                try {
                    // Check if max retries exceeded
                    if (invoice.getVerifactuRetryCount() >= MAX_RETRY_ATTEMPTS) {
                        markAsFailed(invoice);
                        failedCount++;
                        continue;
                    }

                    // Increment retry count and ensure status is PENDING
                    // The VeriFactu batch scheduler will pick up PENDING invoices
                    if ("PROCESSING".equals(invoice.getVerifactuStatus())) {
                        // If stuck in PROCESSING, move back to PENDING for retry
                        log.debug("Invoice {} stuck in PROCESSING, marking as PENDING for retry (attempt {})",
                                invoice.getId(), invoice.getVerifactuRetryCount() + 1);
                        invoice.setVerifactuStatus("PENDING");
                        invoice.incrementRetryCount();
                        invoiceRepository.save(invoice);
                        retryCount++;
                    } else {
                        // Already PENDING, skip
                        skippedCount++;
                    }

                } catch (Exception e) {
                    log.warn("Error processing invoice {} for retry: {}", invoice.getId(), e.getMessage());
                }
            }

            log.info(
                    "VeriFactu retry job completed. Retried: {}, Failed (max attempts): {}, Skipped (already pending): {}",
                    retryCount, failedCount, skippedCount);

        } catch (Exception e) {
            log.error("Error in VeriFactu retry job", e);
        }
    }

    private void markAsFailed(Invoice invoice) {
        String errorMessage = "Max retry attempts (" + MAX_RETRY_ATTEMPTS
                + ") exceeded. Please check invoice data and retry manually.";

        invoice.setVerifactuStatus("FAILED");
        invoice.setVerifactuError(errorMessage);
        invoiceRepository.save(invoice);

        log.warn("Invoice {} marked as FAILED after {} attempts. Company: {}",
                invoice.getId(), MAX_RETRY_ATTEMPTS, invoice.getCompanyId());

        // Send email notification about the failure
        emailService.sendVerifactuFailureEmail(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getCompanyId(),
                invoice.getVerifactuRetryCount(),
                errorMessage);
    }
}
