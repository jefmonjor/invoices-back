package com.invoices.invoice_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceItemDTO {
    private Integer id;
    private Integer invoiceId;
    private String description;
    private Integer units;
    private BigDecimal discountPercentage;
    private BigDecimal price;
    private BigDecimal vatPercentage;
    private BigDecimal subtotal;
    private BigDecimal total;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}
