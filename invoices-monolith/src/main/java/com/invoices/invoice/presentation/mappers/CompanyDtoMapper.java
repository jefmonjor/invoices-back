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
                .country(company.getCountry())
                .phone(company.getPhone())
                .email(company.getEmail())
                .iban(company.getIban())
                .build();
    }

    public Company toDomain(CompanyDTO dto) {
        if (dto == null) {
            return null;
        }

        return new Company(
                dto.getId(),
                dto.getBusinessName(),
                dto.getTaxId(),
                dto.getAddress(),
                dto.getCity(),
                dto.getPostalCode(),
                dto.getProvince(),
                dto.getCountry(),
                dto.getPhone(),
                dto.getEmail(),
                dto.getIban(),
                null,
                null);
    }
}
