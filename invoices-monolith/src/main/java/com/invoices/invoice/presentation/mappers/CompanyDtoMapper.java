package com.invoices.invoice.presentation.mappers;

import com.invoices.invoice.domain.entities.Company;
import com.invoices.invoice.dto.CompanyDTO;
import org.springframework.stereotype.Component;

/**
 * Mapper between domain Company and CompanyDTO.
 * Keeps domain and presentation layers decoupled.
 */
@Component
public class CompanyDtoMapper {

    public CompanyDTO toDto(Company company) {
        if (company == null) {
            return null;
        }

        return CompanyDTO.builder()
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
            .build();
    }
}
