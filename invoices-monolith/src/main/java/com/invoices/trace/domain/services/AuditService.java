package com.invoices.trace.domain.services;

import com.invoices.trace.domain.entities.AuditLog;
import com.invoices.trace.domain.ports.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Asynchronously logs an event to the audit log.
     * Uses REQUIRES_NEW propagation to ensure the log is committed even if the main
     * transaction fails (optional, depending on requirements).
     * For now, we use default propagation but async execution.
     *
     * @param companyId     The company ID
     * @param eventType     The type of event
     * @param invoiceId     The invoice ID (optional)
     * @param invoiceNumber The invoice number (optional)
     * @param clientId      The client ID (optional)
     * @param clientEmail   The client email (optional)
     * @param status        The status (optional)
     * @param eventData     Additional data in JSON format (optional)
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logEvent(Long companyId, String eventType, Long invoiceId, String invoiceNumber,
            Long clientId, String clientEmail, String status, String eventData) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .companyId(companyId)
                    .eventType(eventType)
                    .invoiceId(invoiceId)
                    .invoiceNumber(invoiceNumber)
                    .clientId(clientId)
                    .clientEmail(clientEmail)
                    .status(status)
                    .eventData(eventData)
                    .createdAt(LocalDateTime.now())
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit log saved: {}", eventType);

        } catch (Exception e) {
            log.error("Failed to save audit log", e);
            // Do not rethrow to avoid affecting the main flow
        }
    }
}
