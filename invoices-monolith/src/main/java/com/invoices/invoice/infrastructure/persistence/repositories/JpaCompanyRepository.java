package com.invoices.invoice.infrastructure.persistence.repositories;

import com.invoices.invoice.infrastructure.persistence.entities.CompanyJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA repository for Company.
 * Infrastructure layer - database access.
 * No @Repository needed - Spring Data JPA auto-detects this interface.
 */
public interface JpaCompanyRepository extends JpaRepository<CompanyJpaEntity, Long> {

    Optional<CompanyJpaEntity> findByTaxId(String taxId);

    boolean existsByTaxId(String taxId);
}
