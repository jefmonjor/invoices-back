package com.invoices.company.presentation.dto;

import com.invoices.invoice.domain.entities.Company;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyDto {
    private Long id;
    private String businessName;
    private String taxId;
    private String address;
    private String city;
    private String postalCode;
    private String province;
    private String phone;
    private String email;
    private String role; // Role of the current user in this company
    private boolean isDefault;

    public static CompanyDto fromEntity(Company company) {
        return CompanyDto.builder()
                .id(company.getId())
                .businessName(company.getBusinessName())
                .taxId(company.getTaxId())
                .address(company.getAddress())
                .city(company.getCity())
                .postalCode(company.getPostalCode())
                .province(company.getProvince())
                .phone(company.getPhone())
                .email(company.getEmail())
                .build();
    }

    // Overload to include user specific details
    public static CompanyDto fromEntity(Company company, String role, boolean isDefault) {
        CompanyDto dto = fromEntity(company);
        dto.setRole(role);
        dto.setDefault(isDefault);
        return dto;
    }
}
