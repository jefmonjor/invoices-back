package com.invoices.invoice_service.dto;

import com.invoices.invoice_service.entity.InvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateInvoiceRequest {
    private LocalDate dueDate;
    private InvoiceStatus status;
    private String notes;
}
