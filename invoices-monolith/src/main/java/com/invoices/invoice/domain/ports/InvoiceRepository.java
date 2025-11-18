package com.invoices.invoice.domain.ports;

import com.invoices.invoice.domain.entities.Invoice;

import java.util.List;
import java.util.Optional;

/**
 * Port (interface) for invoice persistence.
 * Domain layer defines the contract, infrastructure implements it.
 * Dependency Inversion Principle (SOLID).
 */
public interface InvoiceRepository {

    Optional<Invoice> findById(Long id);

    List<Invoice> findByUserId(Long userId);

    List<Invoice> findAll();

    Invoice save(Invoice invoice);

    void deleteById(Long id);

    boolean existsById(Long id);
}
