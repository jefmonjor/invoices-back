package com.invoices.company.presentation.dto;

import com.invoices.shared.domain.validation.ValidNif;
import com.invoices.shared.domain.utils.InputSanitizer;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new company.
 * Used when an ADMIN user wants to create an additional company.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCompanyRequest {

    @NotBlank(message = "Business name is required")
    private String businessName;

    @NotBlank(message = "Tax ID (CIF/NIF) is required")
    @ValidNif
    private String taxId;

    @NotBlank(message = "Address is required")
    private String address;

    private String city;
    private String postalCode;
    private String province;

    @Email(message = "Invalid email format")
    private String email;

    private String phone;

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
