package com.invoices.invoice.infrastructure.persistence.mappers;

import com.invoices.invoice.domain.entities.Company;
import com.invoices.invoice.infrastructure.persistence.entities.CompanyJpaEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper between Company domain entity and CompanyJpaEntity.
 * Infrastructure concern - domain doesn't know about JPA.
 */
@Component
public class CompanyJpaMapper {

    public Company toDomain(CompanyJpaEntity jpaEntity) {
        if (jpaEntity == null) {
            return null;
        }

        return new Company(
                jpaEntity.getId(),
                jpaEntity.getBusinessName(),
                jpaEntity.getTaxId(),
                jpaEntity.getAddress(),
                jpaEntity.getCity(),
                jpaEntity.getPostalCode(),
                jpaEntity.getProvince(),
                jpaEntity.getCountry(),
                jpaEntity.getPhone(),
                jpaEntity.getEmail(),
                jpaEntity.getIban(),
                jpaEntity.getLastHash(),
                jpaEntity.getCertRef(),
                jpaEntity.getCertPassword(),
                jpaEntity.getCreatedAt(),
                jpaEntity.getUpdatedAt());
    }

    public CompanyJpaEntity toJpaEntity(Company domain) {
        if (domain == null) {
            return null;
        }

        CompanyJpaEntity jpaEntity = new CompanyJpaEntity();
        jpaEntity.setId(domain.getId());
        jpaEntity.setBusinessName(domain.getBusinessName());
        jpaEntity.setTaxId(domain.getTaxId());
        jpaEntity.setAddress(domain.getAddress());
        jpaEntity.setCity(domain.getCity());
        jpaEntity.setPostalCode(domain.getPostalCode());
        jpaEntity.setProvince(domain.getProvince());
        jpaEntity.setCountry(domain.getCountry());
        jpaEntity.setPhone(domain.getPhone());
        jpaEntity.setEmail(domain.getEmail());
        jpaEntity.setIban(domain.getIban());
        jpaEntity.setLastHash(domain.getLastHash());
        jpaEntity.setCertRef(domain.getCertRef());
        jpaEntity.setCertPassword(domain.getCertPassword());

        return jpaEntity;
    }
}
