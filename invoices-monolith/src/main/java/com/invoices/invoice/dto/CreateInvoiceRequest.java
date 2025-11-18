package com.invoices.invoice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
    private String invoiceNumber;

    private BigDecimal irpfPercentage;

    private BigDecimal rePercentage;

    private String notes;

    @NotEmpty(message = "Invoice must have at least one item")
    @Valid
    private List<CreateInvoiceItemRequest> items;
}
