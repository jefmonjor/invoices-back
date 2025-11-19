package com.invoices.invoice.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotNull(message = "Units are required")
    @Min(value = 1, message = "Units must be at least 1")
    @Max(value = 999999, message = "Units cannot exceed 999999")
    private Integer units;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @DecimalMax(value = "999999999.99", message = "Price cannot exceed 999999999.99")
    private BigDecimal price;

    @NotNull(message = "VAT percentage is required")
    @DecimalMin(value = "0.0", message = "VAT percentage must be non-negative")
    @DecimalMax(value = "100.0", message = "VAT percentage cannot exceed 100%")
    private BigDecimal vatPercentage;

    @DecimalMin(value = "0.0", message = "Discount percentage must be non-negative")
    @DecimalMax(value = "100.0", message = "Discount percentage cannot exceed 100%")
    private BigDecimal discountPercentage;
}
