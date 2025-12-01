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
 * <p>
 * <b>IMMUTABLE FIELDS</b> (validated/rejected by backend if changed):
 * </p>
 * <ul>
 * <li><b>companyId</b> - Cannot be changed after invoice creation (tenant
 * boundary)</li>
 * <li><b>invoiceNumber</b> - Cannot be changed after invoice creation
 * (regulatory requirement)</li>
 * </ul>
 *
 * <p>
 * <b>UPDATABLE FIELDS:</b>
 * </p>
 * <ul>
 * <li>clientId - Can be updated (backend validates client exists)</li>
 * <li>irpfPercentage - Can be updated (affects invoice calculations)</li>
 * <li>rePercentage - Can be updated (affects invoice calculations)</li>
 * <li>settlementNumber - Can be updated</li>
 * <li>notes - Can be updated</li>
 * <li>items - Can be updated (replaces all items)</li>
 * </ul>
 *
 * <p>
 * Fields sent in request but marked immutable will cause backend to return
 * <code>400 Bad Request</code>
 * with message: "Cannot change invoice number" or "Cannot change company ID".
 * </p>
 *
 * <p>
 * Ignores unknown fields (like 'id', 'status', 'createdAt', etc.) from
 * frontend.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateInvoiceRequest {

    // IMMUTABLE FIELDS - included for validation, rejected if different from
    // existing invoice
    /** Company ID (IMMUTABLE) - Backend validates this matches existing invoice */
    private Long companyId;

    /**
     * Invoice Number (IMMUTABLE) - Backend validates this matches existing invoice
     */
    private String invoiceNumber;

    // UPDATABLE FIELDS
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
