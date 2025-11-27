package com.invoices.company.presentation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import com.invoices.shared.domain.utils.InputSanitizer;

@Data
public class UpdateCompanyRequest {
    @NotBlank
    private String businessName;

    @NotBlank
    private String address;

    @NotBlank
    private String city;

    @NotBlank
    private String postalCode;

    @NotBlank
    private String province;

    private String phone;

    @Email
    private String email;

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
