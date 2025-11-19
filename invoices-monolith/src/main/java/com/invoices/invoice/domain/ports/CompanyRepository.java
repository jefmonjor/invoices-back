package com.invoices.invoice.domain.ports;

import com.invoices.invoice.domain.entities.Company;

import java.util.List;
import java.util.Optional;

/**
 * Port for Company repository.
 * Domain interface - implementations are in infrastructure layer.
 */
public interface CompanyRepository {

    Company save(Company company);

    Optional<Company> findById(Long id);

    Optional<Company> findByTaxId(String taxId);

    List<Company> findAll();

    void deleteById(Long id);

    boolean existsById(Long id);
}
