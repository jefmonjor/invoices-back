package com.invoices.invoice.infrastructure.persistence.repositories;

import com.invoices.invoice.infrastructure.persistence.entities.CompanyJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Spring Data JPA repository for Company.
 * Infrastructure layer - database access.
 * No @Repository needed - Spring Data JPA auto-detects this interface.
 */
public interface JpaCompanyRepository extends JpaRepository<CompanyJpaEntity, Long> {

    Optional<CompanyJpaEntity> findByTaxId(String taxId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CompanyJpaEntity c WHERE c.id = :id")
    Optional<CompanyJpaEntity> findByIdWithLock(@Param("id") Long id);

    boolean existsByTaxId(String taxId);
}
