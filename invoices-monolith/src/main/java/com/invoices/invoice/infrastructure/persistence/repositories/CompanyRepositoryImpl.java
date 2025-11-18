package com.invoices.invoice.infrastructure.persistence.repositories;

import com.invoices.invoice.domain.entities.Company;
import com.invoices.invoice.domain.ports.CompanyRepository;
import com.invoices.invoice.infrastructure.persistence.mappers.CompanyJpaMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of CompanyRepository port.
 * Adapter between domain and JPA persistence.
 */
@Repository
public class CompanyRepositoryImpl implements CompanyRepository {

    private final JpaCompanyRepository jpaRepository;
    private final CompanyJpaMapper mapper;

    public CompanyRepositoryImpl(JpaCompanyRepository jpaRepository, CompanyJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Company save(Company company) {
        var jpaEntity = mapper.toJpaEntity(company);
        var savedEntity = jpaRepository.save(jpaEntity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Company> findById(Long id) {
        return jpaRepository.findById(id)
            .map(mapper::toDomain);
    }

    @Override
    public Optional<Company> findByTaxId(String taxId) {
        return jpaRepository.findByTaxId(taxId)
            .map(mapper::toDomain);
    }

    @Override
    public List<Company> findAll() {
        return jpaRepository.findAll().stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return jpaRepository.existsById(id);
    }
}
