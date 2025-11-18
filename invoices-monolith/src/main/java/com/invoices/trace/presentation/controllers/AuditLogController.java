package com.invoices.trace.presentation.controllers;

import com.invoices.trace.domain.entities.AuditLog;
import com.invoices.trace.domain.usecases.*;
import com.invoices.trace.presentation.dto.AuditLogDTO;
import com.invoices.trace.presentation.mappers.AuditLogDtoMapper;
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
import java.util.stream.Collectors;

/**
 * REST controller for audit log management operations (Clean Architecture).
 * Uses Use Cases from domain layer instead of service layer.
 */
@RestController
@RequestMapping("/api/traces")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Trace Service", description = "Audit log management API for tracking invoice events")
public class AuditLogController {

    private final GetAuditLogByIdUseCase getAuditLogByIdUseCase;
    private final GetAuditLogsByInvoiceUseCase getAuditLogsByInvoiceUseCase;
    private final GetAuditLogsByClientUseCase getAuditLogsByClientUseCase;
    private final GetAuditLogsByEventTypeUseCase getAuditLogsByEventTypeUseCase;
    private final GetAllAuditLogsUseCase getAllAuditLogsUseCase;
    private final AuditLogDtoMapper mapper;

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

        // If specific filters are provided, return a list (use cases handle filtering)
        if (invoiceId != null) {
            List<AuditLog> logs = getAuditLogsByInvoiceUseCase.execute(invoiceId);
            List<AuditLogDTO> logDTOs = logs.stream()
                    .map(mapper::toDTO)
                    .collect(Collectors.toList());
            log.info("Retrieved {} audit logs for invoice {}", logDTOs.size(), invoiceId);
            return ResponseEntity.ok(logDTOs);
        }

        if (clientId != null) {
            List<AuditLog> logs = getAuditLogsByClientUseCase.execute(clientId);
            List<AuditLogDTO> logDTOs = logs.stream()
                    .map(mapper::toDTO)
                    .collect(Collectors.toList());
            log.info("Retrieved {} audit logs for client {}", logDTOs.size(), clientId);
            return ResponseEntity.ok(logDTOs);
        }

        if (eventType != null) {
            List<AuditLog> logs = getAuditLogsByEventTypeUseCase.execute(eventType);
            List<AuditLogDTO> logDTOs = logs.stream()
                    .map(mapper::toDTO)
                    .collect(Collectors.toList());
            log.info("Retrieved {} audit logs for event type '{}'", logDTOs.size(), eventType);
            return ResponseEntity.ok(logDTOs);
        }

        // Otherwise, return paginated results using GetAllAuditLogsUseCase
        Sort.Direction direction = sortDir.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<AuditLog> logsPage = getAllAuditLogsUseCase.execute(pageable);
        Page<AuditLogDTO> logDTOsPage = logsPage.map(mapper::toDTO);

        log.info("Retrieved {} total audit logs (page {}/{})",
                logsPage.getTotalElements(), page, logsPage.getTotalPages());
        return ResponseEntity.ok(logDTOsPage);
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

        AuditLog auditLog = getAuditLogByIdUseCase.execute(id);
        AuditLogDTO auditLogDTO = mapper.toDTO(auditLog);

        log.info("Audit log {} retrieved successfully", id);
        return ResponseEntity.ok(auditLogDTO);
    }
}
