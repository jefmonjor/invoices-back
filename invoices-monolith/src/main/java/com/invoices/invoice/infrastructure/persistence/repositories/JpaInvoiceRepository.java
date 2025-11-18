package com.invoices.invoice.infrastructure.persistence.repositories;

import com.invoices.invoice.infrastructure.persistence.entities.InvoiceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository interface.
 * Infrastructure layer - persistence mechanism.
 */
@Repository
public interface JpaInvoiceRepository extends JpaRepository<InvoiceJpaEntity, Long> {

    List<InvoiceJpaEntity> findByUserId(Long userId);

    boolean existsByInvoiceNumber(String invoiceNumber);
}
