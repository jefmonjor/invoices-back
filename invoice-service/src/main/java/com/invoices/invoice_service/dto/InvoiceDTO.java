package com.invoices.invoice_service.dto;

import com.invoices.invoice_service.entity.InvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceDTO {
    private Long id;
    private String invoiceNumber;
    private Long clientId;
    private String clientEmail;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal total;
    private InvoiceStatus status;
    private String notes;
    private List<InvoiceItemDTO> items;
    private LocalDateTime createdAt;
}
