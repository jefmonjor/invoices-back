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
    private String iban;
    private String logoUrl;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;
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
                .iban(company.getIban())
                .logoUrl(company.getLogoUrl())
                .createdAt(company.getCreatedAt())
                .updatedAt(company.getUpdatedAt())
                .build();
    }

    // Overload with URL resolver for logo
    public static CompanyDto fromEntity(Company company,
            com.invoices.document.domain.services.StorageUrlResolver urlResolver) {
        String resolvedLogoUrl = urlResolver != null && company.getLogoUrl() != null
                ? urlResolver.resolvePublicUrl(company.getLogoUrl())
                : company.getLogoUrl();

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
                .iban(company.getIban())
                .logoUrl(resolvedLogoUrl)
                .createdAt(company.getCreatedAt())
                .updatedAt(company.getUpdatedAt())
                .build();
    }

    // Overload to include user specific details
    public static CompanyDto fromEntity(Company company, String role, boolean isDefault) {
        CompanyDto dto = fromEntity(company);
        dto.setRole(role);
        dto.setDefault(isDefault);
        return dto;
    }

    // Overload with URL resolver and user details
    public static CompanyDto fromEntity(Company company, String role, boolean isDefault,
            com.invoices.document.domain.services.StorageUrlResolver urlResolver) {
        CompanyDto dto = fromEntity(company, urlResolver);
        dto.setRole(role);
        dto.setDefault(isDefault);
        return dto;
    }
}
