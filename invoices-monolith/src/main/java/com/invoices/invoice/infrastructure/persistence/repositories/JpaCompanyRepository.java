package com.invoices.invoice_service.infrastructure.persistence.repositories;

import com.invoices.invoice_service.infrastructure.persistence.entities.CompanyJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for Company.
 * Infrastructure layer - database access.
 */
@Repository
public interface JpaCompanyRepository extends JpaRepository<CompanyJpaEntity, Long> {

    Optional<CompanyJpaEntity> findByTaxId(String taxId);

    boolean existsByTaxId(String taxId);
}
