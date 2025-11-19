package com.invoices.invoice.infrastructure.persistence.repositories;

import com.invoices.invoice.infrastructure.persistence.entities.ClientJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA repository for Client.
 * Infrastructure layer - database access.
 * No @Repository needed - Spring Data JPA auto-detects this interface.
 */
public interface JpaClientRepository extends JpaRepository<ClientJpaEntity, Long> {

    Optional<ClientJpaEntity> findByTaxId(String taxId);

    boolean existsByTaxId(String taxId);
}
