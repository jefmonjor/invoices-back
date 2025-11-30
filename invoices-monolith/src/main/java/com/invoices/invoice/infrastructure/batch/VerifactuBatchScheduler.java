package com.invoices.invoice.infrastructure.batch;

import com.invoices.invoice.dto.BatchSummary;
import com.invoices.invoice.infrastructure.persistence.repositories.JpaInvoiceRepository;
import com.invoices.invoice.infrastructure.services.SmtpEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Batch scheduler for VeriFactu operations
 * - Daily retry of failed verifications at 02:00 AM
 * - Weekly metrics report on Mondays at 09:00 AM
 */
@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "verifactu.batch", name = "enabled", havingValue = "true", matchIfMissing = false)
public class VerifactuBatchScheduler {

    private final JpaInvoiceRepository invoiceRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SmtpEmailService emailService;

    /**
     * Scheduled task to retry failed verifications
     * Runs daily at 02:00 AM
     */
    @Scheduled(cron = "${verifactu.batch.retry-cron:0 0 2 * * ?}")
    public void retryFailedVerifications() {
        log.info("[VeriFactu Batch] Starting daily retry process at {}", LocalDateTime.now());

        try {
            // Find invoices with REJECTED, FAILED, or TIMEOUT status
            // that are older than 24 hours
            LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);

            var invoiceEntities = invoiceRepository.findByVerifactuStatusInAndUpdatedAtBefore(
                    List.of("REJECTED", "FAILED", "TIMEOUT"),
                    cutoffTime);

            log.info("[VeriFactu Batch] Found {} invoices to retry", invoiceEntities.size());

            // Group by Company ID to report separately (optional, but good for metrics)
            Map<Long, Integer> companyRetryCounts = new HashMap<>();

            int requeued = 0;
            for (var invoiceEntity : invoiceEntities) {
                try {
                    // Create retry event
                    Map<String, Object> event = new HashMap<>();
                    event.put("invoiceId", invoiceEntity.getId());
                    event.put("companyId", invoiceEntity.getCompanyId()); // Add company context
                    event.put("eventType", "RETRY_VERIFICATION");
                    event.put("batchRetry", true);
                    event.put("timestamp", System.currentTimeMillis());

                    // Add to Redis stream
                    redisTemplate.opsForStream().add("verifactu-queue", event);
                    requeued++;

                    companyRetryCounts.merge(invoiceEntity.getCompanyId(), 1, (a, b) -> a + b);

                    log.debug("[VeriFactu Batch] Requeued invoice {} for retry (Company {})",
                            invoiceEntity.getId(), invoiceEntity.getCompanyId());
                } catch (Exception e) {
                    log.error("[VeriFactu Batch] Error requeueing invoice {}: {}",
                            invoiceEntity.getId(), e.getMessage());
                }
            }

            log.info("[VeriFactu Batch] Successfully requeued {} invoices for verification", requeued);

            // Log retries per company
            companyRetryCounts.forEach(
                    (companyId, count) -> log.info("[VeriFactu Batch] Company {}: {} retries", companyId, count));

            // Update metrics
            updateBatchMetrics(invoiceEntities.size(), requeued);

            // Calculate critical pending invoices (>48h)
            long criticalPending = invoiceRepository.countByVerifactuStatusAndUpdatedAtBefore(
                    "NOT_SENT", LocalDateTime.now().minusHours(48));

            // Build batch summary
            BatchSummary summary = BatchSummary.builder()
                    .timestamp(LocalDateTime.now())
                    .totalProcessed(invoiceEntities.size())
                    .successful(requeued)
                    .failed(invoiceEntities.size() - requeued)
                    .criticalPending((int) criticalPending)
                    .successRate(invoiceEntities.size() > 0 ? (double) requeued / invoiceEntities.size() * 100 : 0.0)
                    .build();

            // Send email summary
            emailService.sendBatchSummaryEmail(summary);

        } catch (Exception e) {
            log.error("[VeriFactu Batch] Error in retry process", e);
        }
    }

    /**
     * Weekly metrics report
     * Runs every Monday at 09:00 AM
     */
    @Scheduled(cron = "${verifactu.batch.weekly-report-cron:0 0 9 * * MON}")
    public void weeklyVerifactuReport() {
        log.info("[VeriFactu Batch] Generating weekly report at {}", LocalDateTime.now());

        try {
            LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);

            // Calculate weekly metrics
            long totalVerifications = invoiceRepository.countByCreatedAtAfter(weekAgo);
            long accepted = invoiceRepository.countByVerifactuStatusAndCreatedAtAfter("ACCEPTED", weekAgo);
            long rejected = invoiceRepository.countByVerifactuStatusAndCreatedAtAfter("REJECTED", weekAgo);
            long failed = invoiceRepository.countByVerifactuStatusAndCreatedAtAfter("FAILED", weekAgo);
            long pending = invoiceRepository.countByVerifactuStatusIn(List.of("PENDING", "PROCESSING"));

            double successRate = totalVerifications > 0
                    ? (double) accepted / totalVerifications * 100
                    : 0.0;

            log.info("[VeriFactu Weekly Report] " +
                    "Total: {}, Accepted: {}, Rejected: {}, Failed: {}, Pending: {}, Success Rate: {:.2f}%",
                    totalVerifications, accepted, rejected, failed, pending, successRate);

            // Store metrics in Redis
            String metricsKey = "verifactu:weekly-report:" + LocalDateTime.now().toLocalDate();
            Map<String, String> metrics = new HashMap<>();
            metrics.put("total", String.valueOf(totalVerifications));
            metrics.put("accepted", String.valueOf(accepted));
            metrics.put("rejected", String.valueOf(rejected));
            metrics.put("failed", String.valueOf(failed));
            metrics.put("pending", String.valueOf(pending));
            metrics.put("successRate", String.format("%.2f", successRate));

            redisTemplate.opsForHash().putAll(metricsKey, metrics);

        } catch (Exception e) {
            log.error("[VeriFactu Batch] Error generating weekly report", e);
        }
    }

    private void updateBatchMetrics(int found, int requeued) {
        try {
            String key = "verifactu:batch:metrics";
            redisTemplate.opsForHash().increment(key, "total_batch_runs", 1);
            redisTemplate.opsForHash().increment(key, "total_found", found);
            redisTemplate.opsForHash().increment(key, "total_requeued", requeued);
        } catch (Exception e) {
            log.error("[VeriFactu Batch] Error updating metrics", e);
        }
    }
}
