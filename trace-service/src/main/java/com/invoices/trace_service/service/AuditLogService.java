package com.invoices.trace_service.service;

import com.invoices.trace_service.dto.AuditLogDTO;
import com.invoices.trace_service.entity.AuditLog;
import com.invoices.trace_service.exception.AuditLogNotFoundException;
import com.invoices.trace_service.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public List<AuditLogDTO> getLogsByInvoiceId(Long invoiceId) {
        log.info("Fetching audit logs for invoice ID: {}", invoiceId);
        List<AuditLog> logs = auditLogRepository.findByInvoiceIdOrderByCreatedAtDesc(invoiceId);
        log.info("Found {} audit logs for invoice ID: {}", logs.size(), invoiceId);
        return logs.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<AuditLogDTO> getLogsByClientId(Long clientId) {
        log.info("Fetching audit logs for client ID: {}", clientId);
        List<AuditLog> logs = auditLogRepository.findByClientIdOrderByCreatedAtDesc(clientId);
        log.info("Found {} audit logs for client ID: {}", logs.size(), clientId);
        return logs.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<AuditLogDTO> getLogsByEventType(String eventType) {
        log.info("Fetching audit logs for event type: {}", eventType);
        List<AuditLog> logs = auditLogRepository.findByEventTypeOrderByCreatedAtDesc(eventType);
        log.info("Found {} audit logs for event type: {}", logs.size(), eventType);
        return logs.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public Page<AuditLogDTO> getAllLogs(Pageable pageable) {
        log.info("Fetching all audit logs with pagination: page={}, size={}",
                pageable.getPageNumber(), pageable.getPageSize());
        Page<AuditLog> logsPage = auditLogRepository.findAll(pageable);
        log.info("Found {} total audit logs", logsPage.getTotalElements());
        return logsPage.map(this::mapToDTO);
    }

    public AuditLogDTO getLogById(Long id) {
        log.info("Fetching audit log with ID: {}", id);
        AuditLog auditLog = auditLogRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Audit log not found with ID: {}", id);
                    return new AuditLogNotFoundException(id);
                });
        return mapToDTO(auditLog);
    }

    private AuditLogDTO mapToDTO(AuditLog auditLog) {
        return AuditLogDTO.builder()
                .id(auditLog.getId())
                .eventType(auditLog.getEventType())
                .invoiceId(auditLog.getInvoiceId())
                .invoiceNumber(auditLog.getInvoiceNumber())
                .clientId(auditLog.getClientId())
                .clientEmail(auditLog.getClientEmail())
                .total(auditLog.getTotal())
                .status(auditLog.getStatus())
                .eventData(auditLog.getEventData())
                .createdAt(auditLog.getCreatedAt())
                .build();
    }
}
