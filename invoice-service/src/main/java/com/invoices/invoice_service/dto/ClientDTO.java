package com.invoices.invoice_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Client (Invoice Recipient).
 * Matches OpenAPI specification v2.0.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientDTO {
    private Long id;
    private String businessName;
    private String taxId;
    private String address;
    private String city;
    private String postalCode;
    private String province;
    private String phone;
    private String email;
}
