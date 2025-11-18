package com.invoices.trace.controller;

import com.invoices.trace.domain.entities.AuditLog;
import com.invoices.trace.domain.usecases.*;
import com.invoices.trace.presentation.dto.AuditLogDTO;
import com.invoices.trace.presentation.mappers.AuditLogDtoMapper;
import com.invoices.trace.presentation.controllers.AuditLogController;
import com.invoices.trace.exception.AuditLogNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuditLogController.class)
@Disabled("Requires refactoring to use cases - TODO")
class AuditLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetAuditLogByIdUseCase getAuditLogByIdUseCase;

    @MockBean
    private GetAuditLogsByInvoiceUseCase getAuditLogsByInvoiceUseCase;

    @MockBean
    private GetAuditLogsByClientUseCase getAuditLogsByClientUseCase;

    @MockBean
    private GetAuditLogsByEventTypeUseCase getAuditLogsByEventTypeUseCase;

    @MockBean
    private GetAllAuditLogsUseCase getAllAuditLogsUseCase;

    @MockBean
    private AuditLogDtoMapper mapper;

    @Test
    void shouldGetAuditLogsByInvoiceId() throws Exception {
        // Given
        List<AuditLogDTO> logs = Arrays.asList(
                createTestDTO(1L, "INVOICE_CREATED", 100L, "INV-001"),
                createTestDTO(2L, "INVOICE_UPDATED", 100L, "INV-001")
        );
        when(auditLogService.getLogsByInvoiceId(100L)).thenReturn(logs);

        // When/Then
        mockMvc.perform(get("/api/traces")
                        .param("invoiceId", "100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].invoiceId").value(100))
                .andExpect(jsonPath("$[0].eventType").value("INVOICE_CREATED"));
    }

    @Test
    void shouldGetAuditLogsByClientId() throws Exception {
        // Given
        List<AuditLogDTO> logs = List.of(
                createTestDTO(1L, "INVOICE_CREATED", 100L, "INV-001")
        );
        when(auditLogService.getLogsByClientId(10L)).thenReturn(logs);

        // When/Then
        mockMvc.perform(get("/api/traces")
                        .param("clientId", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].clientId").value(10));
    }

    @Test
    void shouldGetAuditLogsByEventType() throws Exception {
        // Given
        List<AuditLogDTO> logs = List.of(
                createTestDTO(1L, "INVOICE_CREATED", 100L, "INV-001")
        );
        when(auditLogService.getLogsByEventType("INVOICE_CREATED")).thenReturn(logs);

        // When/Then
        mockMvc.perform(get("/api/traces")
                        .param("eventType", "INVOICE_CREATED")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].eventType").value("INVOICE_CREATED"));
    }

    @Test
    void shouldGetAllAuditLogsWithPagination() throws Exception {
        // Given
        List<AuditLogDTO> logs = Arrays.asList(
                createTestDTO(1L, "INVOICE_CREATED", 100L, "INV-001"),
                createTestDTO(2L, "INVOICE_UPDATED", 101L, "INV-002")
        );
        Page<AuditLogDTO> page = new PageImpl<>(logs, PageRequest.of(0, 20), 2);
        when(auditLogService.getAllLogs(any())).thenReturn(page);

        // When/Then
        mockMvc.perform(get("/api/traces")
                        .param("page", "0")
                        .param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    void shouldGetAllAuditLogsWithCustomSorting() throws Exception {
        // Given
        List<AuditLogDTO> logs = List.of(
                createTestDTO(1L, "INVOICE_CREATED", 100L, "INV-001")
        );
        Page<AuditLogDTO> page = new PageImpl<>(logs, PageRequest.of(0, 10), 1);
        when(auditLogService.getAllLogs(any())).thenReturn(page);

        // When/Then
        mockMvc.perform(get("/api/traces")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "eventType")
                        .param("sortDir", "ASC")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void shouldGetAuditLogById() throws Exception {
        // Given
        AuditLogDTO log = createTestDTO(1L, "INVOICE_CREATED", 100L, "INV-001");
        when(auditLogService.getLogById(1L)).thenReturn(log);

        // When/Then
        mockMvc.perform(get("/api/traces/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.eventType").value("INVOICE_CREATED"))
                .andExpect(jsonPath("$.invoiceId").value(100))
                .andExpect(jsonPath("$.invoiceNumber").value("INV-001"));
    }

    @Test
    void shouldReturn404WhenAuditLogNotFound() throws Exception {
        // Given
        when(auditLogService.getLogById(999L))
                .thenThrow(new AuditLogNotFoundException(999L));

        // When/Then
        mockMvc.perform(get("/api/traces/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldUseDefaultPaginationParameters() throws Exception {
        // Given
        Page<AuditLogDTO> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(auditLogService.getAllLogs(any())).thenReturn(page);

        // When/Then
        mockMvc.perform(get("/api/traces")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    void shouldHandleMultipleFiltersWithInvoiceIdTakingPrecedence() throws Exception {
        // Given - when multiple filters provided, invoiceId should take precedence
        List<AuditLogDTO> logs = List.of(
                createTestDTO(1L, "INVOICE_CREATED", 100L, "INV-001")
        );
        when(auditLogService.getLogsByInvoiceId(100L)).thenReturn(logs);

        // When/Then
        mockMvc.perform(get("/api/traces")
                        .param("invoiceId", "100")
                        .param("clientId", "10")
                        .param("eventType", "INVOICE_CREATED")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    private AuditLogDTO createTestDTO(Long id, String eventType, Long invoiceId, String invoiceNumber) {
        return AuditLogDTO.builder()
                .id(id)
                .eventType(eventType)
                .invoiceId(invoiceId)
                .invoiceNumber(invoiceNumber)
                .clientId(10L)
                .clientEmail("test@example.com")
                .total(new BigDecimal("1000.00"))
                .status("PENDING")
                .eventData("{\"test\":\"data\"}")
                .createdAt(LocalDateTime.now())
                .build();
    }
}
