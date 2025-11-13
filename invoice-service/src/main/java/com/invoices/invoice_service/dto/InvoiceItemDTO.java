package com.invoices.invoice_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for Invoice Item.
 * Matches OpenAPI specification v2.0.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceItemDTO {
    private Long id;
    private Long invoiceId;
    private String description;
    private Integer units;
    private BigDecimal discountPercentage;
    private BigDecimal price;
    private BigDecimal vatPercentage;
    private BigDecimal subtotal;
    private BigDecimal total;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
