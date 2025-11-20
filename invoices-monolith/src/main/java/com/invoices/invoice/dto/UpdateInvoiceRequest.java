package com.invoices.invoice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request DTO for updating an existing invoice.
 * Matches OpenAPI specification v2.0.
 *
 * IMMUTABLE FIELDS (validated by backend):
 * - companyId: Cannot be changed after invoice creation
 * - invoiceNumber: Cannot be changed after invoice creation
 *
 * These fields can be sent in the request but if they differ from the
 * existing invoice values, an IllegalArgumentException will be thrown.
 *
 * UPDATABLE FIELDS:
 * - clientId: Can be updated (backend validates client exists)
 * - irpfPercentage: Can be updated (affects invoice calculations)
 * - rePercentage: Can be updated (affects invoice calculations)
 * - settlementNumber: Can be updated
 * - notes: Can be updated
 * - items: Can be updated (replaces all items)
 *
 * Ignores unknown fields (like 'id', 'status', 'createdAt', etc.) from
 * frontend.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateInvoiceRequest {

    // Immutable fields (included for deserialization but not updated)
    private Long companyId;
    private String invoiceNumber;

    // Updatable fields
    private Long clientId;

    @Size(max = 50, message = "Settlement number must not exceed 50 characters")
    private String settlementNumber;

    @DecimalMin(value = "0.0", message = "IRPF percentage must be non-negative")
    @DecimalMax(value = "100.0", message = "IRPF percentage cannot exceed 100%")
    private BigDecimal irpfPercentage;

    @DecimalMin(value = "0.0", message = "RE percentage must be non-negative")
    @DecimalMax(value = "100.0", message = "RE percentage cannot exceed 100%")
    private BigDecimal rePercentage;

    @Size(max = 5000, message = "Notes must not exceed 5000 characters")
    private String notes;

    @Valid
    private List<CreateInvoiceItemRequest> items;
}
