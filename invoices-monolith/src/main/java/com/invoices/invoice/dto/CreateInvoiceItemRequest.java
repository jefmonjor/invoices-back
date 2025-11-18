package com.invoices.invoice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for creating an invoice item.
 * Matches OpenAPI specification v2.0.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateInvoiceItemRequest {

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Units are required")
    @Min(value = 1, message = "Units must be at least 1")
    private Integer units;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;

    @NotNull(message = "VAT percentage is required")
    @DecimalMin(value = "0.0", message = "VAT percentage must be non-negative")
    private BigDecimal vatPercentage;

    private BigDecimal discountPercentage;
}
