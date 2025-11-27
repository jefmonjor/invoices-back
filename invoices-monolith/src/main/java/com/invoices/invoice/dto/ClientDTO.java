package com.invoices.invoice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.invoices.shared.domain.validation.ValidNif;
import com.invoices.shared.domain.utils.InputSanitizer;

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

    @ValidNif
    private String taxId;
    private String address;
    private String city;
    private String postalCode;
    private String province;
    private String country;
    private String phone;
    private String email;
    private Long companyId;

    public void setBusinessName(String businessName) {
        this.businessName = InputSanitizer.sanitize(businessName);
    }

    public void setAddress(String address) {
        this.address = InputSanitizer.sanitize(address);
    }

    public void setCity(String city) {
        this.city = InputSanitizer.sanitize(city);
    }

    public void setProvince(String province) {
        this.province = InputSanitizer.sanitize(province);
    }
}
