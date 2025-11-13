package com.invoices.trace_service.controller;

import com.invoices.trace_service.dto.AuditLogDTO;
import com.invoices.trace_service.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/traces")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Trace Service", description = "Audit log management API for tracking invoice events")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @Operation(summary = "Get audit logs", description = "Retrieve audit logs with optional filters and pagination")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Logs retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuditLogDTO.class)))
    })
    public ResponseEntity<?> getAuditLogs(
            @Parameter(description = "Invoice ID filter")
            @RequestParam(required = false) Long invoiceId,

            @Parameter(description = "Client ID filter")
            @RequestParam(required = false) Long clientId,

            @Parameter(description = "Event type filter")
            @RequestParam(required = false) String eventType,

            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Sort field")
            @RequestParam(defaultValue = "createdAt") String sortBy,

            @Parameter(description = "Sort direction (ASC or DESC)")
            @RequestParam(defaultValue = "DESC") String sortDir
    ) {
        log.info("GET /api/traces - invoiceId={}, clientId={}, eventType={}, page={}, size={}",
                invoiceId, clientId, eventType, page, size);

        // If specific filters are provided, return a list
        if (invoiceId != null) {
            List<AuditLogDTO> logs = auditLogService.getLogsByInvoiceId(invoiceId);
            return ResponseEntity.ok(logs);
        }

        if (clientId != null) {
            List<AuditLogDTO> logs = auditLogService.getLogsByClientId(clientId);
            return ResponseEntity.ok(logs);
        }

        if (eventType != null) {
            List<AuditLogDTO> logs = auditLogService.getLogsByEventType(eventType);
            return ResponseEntity.ok(logs);
        }

        // Otherwise, return paginated results
        Sort.Direction direction = sortDir.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<AuditLogDTO> logsPage = auditLogService.getAllLogs(pageable);
        return ResponseEntity.ok(logsPage);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get audit log by ID", description = "Retrieve a specific audit log by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Log retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuditLogDTO.class))),
            @ApiResponse(responseCode = "404", description = "Log not found")
    })
    public ResponseEntity<AuditLogDTO> getAuditLogById(
            @Parameter(description = "Audit log ID", required = true)
            @PathVariable Long id
    ) {
        log.info("GET /api/traces/{} - Fetching audit log", id);
        AuditLogDTO auditLog = auditLogService.getLogById(id);
        return ResponseEntity.ok(auditLog);
    }
}
