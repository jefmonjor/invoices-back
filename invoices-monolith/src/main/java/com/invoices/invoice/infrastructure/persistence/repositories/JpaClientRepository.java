package com.invoices.invoice.infrastructure.persistence.repositories;

import com.invoices.invoice.infrastructure.persistence.entities.ClientJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for Client.
 * Infrastructure layer - database access.
 */
@Repository
public interface JpaClientRepository extends JpaRepository<ClientJpaEntity, Long> {

    Optional<ClientJpaEntity> findByTaxId(String taxId);

    boolean existsByTaxId(String taxId);
}
