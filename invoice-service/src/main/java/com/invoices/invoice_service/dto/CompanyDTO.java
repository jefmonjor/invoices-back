package com.invoices.invoice_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Company (Invoice Issuer).
 * Matches OpenAPI specification v2.0.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyDTO {
    private Long id;
    private String businessName;
    private String taxId;
    private String address;
    private String city;
    private String postalCode;
    private String province;
    private String phone;
    private String email;
    private String iban;
}
