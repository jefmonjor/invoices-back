package com.invoices.invoice.infrastructure.persistence.mappers;

import com.invoices.invoice.domain.entities.Client;
import com.invoices.invoice.infrastructure.persistence.entities.ClientJpaEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper between Client domain entity and ClientJpaEntity.
 * Infrastructure concern - domain doesn't know about JPA.
 */
@Component
public class ClientJpaMapper {

    public Client toDomain(ClientJpaEntity jpaEntity) {
        if (jpaEntity == null) {
            return null;
        }

        return new Client(
            jpaEntity.getId(),
            jpaEntity.getBusinessName(),
            jpaEntity.getTaxId(),
            jpaEntity.getAddress(),
            jpaEntity.getCity(),
            jpaEntity.getPostalCode(),
            jpaEntity.getProvince(),
            jpaEntity.getPhone(),
            jpaEntity.getEmail()
        );
    }

    public ClientJpaEntity toJpaEntity(Client domain) {
        if (domain == null) {
            return null;
        }

        ClientJpaEntity jpaEntity = new ClientJpaEntity();
        jpaEntity.setId(domain.getId());
        jpaEntity.setBusinessName(domain.getBusinessName());
        jpaEntity.setTaxId(domain.getTaxId());
        jpaEntity.setAddress(domain.getAddress());
        jpaEntity.setCity(domain.getCity());
        jpaEntity.setPostalCode(domain.getPostalCode());
        jpaEntity.setProvince(domain.getProvince());
        jpaEntity.setPhone(domain.getPhone());
        jpaEntity.setEmail(domain.getEmail());

        return jpaEntity;
    }
}
