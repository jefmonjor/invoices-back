package com.invoices.trace_service.service;

import com.invoices.trace_service.dto.AuditLogDTO;
import com.invoices.trace_service.entity.AuditLog;
import com.invoices.trace_service.exception.AuditLogNotFoundException;
import com.invoices.trace_service.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    private AuditLog testLog1;
    private AuditLog testLog2;

    @BeforeEach
    void setUp() {
        testLog1 = AuditLog.builder()
                .id(1L)
                .eventType("INVOICE_CREATED")
                .invoiceId(100L)
                .invoiceNumber("INV-2024-001")
                .clientId(10L)
                .clientEmail("client1@example.com")
                .total(new BigDecimal("1000.00"))
                .status("PENDING")
                .eventData("{\"eventType\":\"INVOICE_CREATED\"}")
                .createdAt(LocalDateTime.now())
                .build();

        testLog2 = AuditLog.builder()
                .id(2L)
                .eventType("INVOICE_UPDATED")
                .invoiceId(100L)
                .invoiceNumber("INV-2024-001")
                .clientId(10L)
                .clientEmail("client1@example.com")
                .total(new BigDecimal("1500.00"))
                .status("SENT")
                .eventData("{\"eventType\":\"INVOICE_UPDATED\"}")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void shouldGetLogsByInvoiceId() {
        // Given
        when(auditLogRepository.findByInvoiceIdOrderByCreatedAtDesc(100L))
                .thenReturn(Arrays.asList(testLog2, testLog1));

        // When
        List<AuditLogDTO> result = auditLogService.getLogsByInvoiceId(100L);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getEventType()).isEqualTo("INVOICE_UPDATED");
        assertThat(result.get(1).getEventType()).isEqualTo("INVOICE_CREATED");
        verify(auditLogRepository).findByInvoiceIdOrderByCreatedAtDesc(100L);
    }

    @Test
    void shouldGetLogsByClientId() {
        // Given
        when(auditLogRepository.findByClientIdOrderByCreatedAtDesc(10L))
                .thenReturn(Arrays.asList(testLog2, testLog1));

        // When
        List<AuditLogDTO> result = auditLogService.getLogsByClientId(10L);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(AuditLogDTO::getClientId).containsOnly(10L);
        verify(auditLogRepository).findByClientIdOrderByCreatedAtDesc(10L);
    }

    @Test
    void shouldGetLogsByEventType() {
        // Given
        when(auditLogRepository.findByEventTypeOrderByCreatedAtDesc("INVOICE_CREATED"))
                .thenReturn(List.of(testLog1));

        // When
        List<AuditLogDTO> result = auditLogService.getLogsByEventType("INVOICE_CREATED");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEventType()).isEqualTo("INVOICE_CREATED");
        verify(auditLogRepository).findByEventTypeOrderByCreatedAtDesc("INVOICE_CREATED");
    }

    @Test
    void shouldGetAllLogsWithPagination() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<AuditLog> page = new PageImpl<>(Arrays.asList(testLog1, testLog2), pageable, 2);
        when(auditLogRepository.findAll(pageable)).thenReturn(page);

        // When
        Page<AuditLogDTO> result = auditLogService.getAllLogs(pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        verify(auditLogRepository).findAll(pageable);
    }

    @Test
    void shouldGetLogById() {
        // Given
        when(auditLogRepository.findById(1L)).thenReturn(Optional.of(testLog1));

        // When
        AuditLogDTO result = auditLogService.getLogById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEventType()).isEqualTo("INVOICE_CREATED");
        assertThat(result.getInvoiceId()).isEqualTo(100L);
        verify(auditLogRepository).findById(1L);
    }

    @Test
    void shouldThrowExceptionWhenLogNotFound() {
        // Given
        when(auditLogRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> auditLogService.getLogById(999L))
                .isInstanceOf(AuditLogNotFoundException.class)
                .hasMessageContaining("999");
        verify(auditLogRepository).findById(999L);
    }

    @Test
    void shouldReturnEmptyListWhenNoLogsFound() {
        // Given
        when(auditLogRepository.findByInvoiceIdOrderByCreatedAtDesc(999L))
                .thenReturn(List.of());

        // When
        List<AuditLogDTO> result = auditLogService.getLogsByInvoiceId(999L);

        // Then
        assertThat(result).isEmpty();
        verify(auditLogRepository).findByInvoiceIdOrderByCreatedAtDesc(999L);
    }

    @Test
    void shouldMapAuditLogToDTOCorrectly() {
        // Given
        when(auditLogRepository.findById(1L)).thenReturn(Optional.of(testLog1));

        // When
        AuditLogDTO result = auditLogService.getLogById(1L);

        // Then
        assertThat(result.getId()).isEqualTo(testLog1.getId());
        assertThat(result.getEventType()).isEqualTo(testLog1.getEventType());
        assertThat(result.getInvoiceId()).isEqualTo(testLog1.getInvoiceId());
        assertThat(result.getInvoiceNumber()).isEqualTo(testLog1.getInvoiceNumber());
        assertThat(result.getClientId()).isEqualTo(testLog1.getClientId());
        assertThat(result.getClientEmail()).isEqualTo(testLog1.getClientEmail());
        assertThat(result.getTotal()).isEqualByComparingTo(testLog1.getTotal());
        assertThat(result.getStatus()).isEqualTo(testLog1.getStatus());
        assertThat(result.getEventData()).isEqualTo(testLog1.getEventData());
        assertThat(result.getCreatedAt()).isEqualTo(testLog1.getCreatedAt());
    }
}
