package com.invoices.invoice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
import java.time.LocalDate;

/**
 * Request DTO for creating an invoice item.
 * Matches OpenAPI specification v2.0.
 *
 * Note: Ignores unknown fields (like 'id') to support both CREATE and UPDATE operations.
 * Frontend can send item IDs when updating, but they will be ignored.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
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

    // Extended fields for detailed invoices
    private LocalDate itemDate;

    @Size(max = 50, message = "Vehicle plate must not exceed 50 characters")
    private String vehiclePlate;

    @Size(max = 50, message = "Order number must not exceed 50 characters")
    private String orderNumber;

    @Size(max = 100, message = "Zone must not exceed 100 characters")
    private String zone;

    @DecimalMin(value = "0.0", message = "Gas percentage must be non-negative")
    @DecimalMax(value = "100.0", message = "Gas percentage cannot exceed 100%")
    private BigDecimal gasPercentage;
}
