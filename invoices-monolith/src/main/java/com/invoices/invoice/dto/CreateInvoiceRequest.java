package com.invoices.invoice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request DTO for creating a new invoice.
 * Matches OpenAPI specification v2.0.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateInvoiceRequest {

    @NotNull(message = "Company ID is required")
    private Long companyId;

    @NotNull(message = "Client ID is required")
    private Long clientId;

    @NotBlank(message = "Invoice number is required")
    @Size(max = 50, message = "Invoice number must not exceed 50 characters")
    private String invoiceNumber;

    @DecimalMin(value = "0.0", message = "IRPF percentage must be non-negative")
    @DecimalMax(value = "100.0", message = "IRPF percentage cannot exceed 100%")
    private BigDecimal irpfPercentage;

    @DecimalMin(value = "0.0", message = "RE percentage must be non-negative")
    @DecimalMax(value = "100.0", message = "RE percentage cannot exceed 100%")
    private BigDecimal rePercentage;

    @Size(max = 5000, message = "Notes must not exceed 5000 characters")
    private String notes;

    @NotEmpty(message = "Invoice must have at least one item")
    @Valid
    private List<CreateInvoiceItemRequest> items;
}
