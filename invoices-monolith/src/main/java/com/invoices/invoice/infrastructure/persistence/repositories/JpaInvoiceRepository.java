package com.invoices.invoice.infrastructure.persistence.repositories;

import com.invoices.invoice.infrastructure.persistence.entities.InvoiceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data JPA repository interface.
 * Infrastructure layer - persistence mechanism.
 * No @Repository needed - Spring Data JPA auto-detects this interface.
 */
public interface JpaInvoiceRepository extends JpaRepository<InvoiceJpaEntity, Long> {

    List<InvoiceJpaEntity> findByUserId(Long userId);

    boolean existsByInvoiceNumber(String invoiceNumber);
}
