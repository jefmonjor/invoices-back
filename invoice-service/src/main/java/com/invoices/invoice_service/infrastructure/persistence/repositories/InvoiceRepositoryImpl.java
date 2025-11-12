package com.invoices.invoice_service.infrastructure.persistence.repositories;

import com.invoices.invoice_service.domain.entities.Invoice;
import com.invoices.invoice_service.domain.ports.InvoiceRepository;
import com.invoices.invoice_service.infrastructure.persistence.entities.InvoiceJpaEntity;
import com.invoices.invoice_service.infrastructure.persistence.mappers.InvoiceJpaMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of InvoiceRepository port.
 * Adapter that bridges domain and infrastructure.
 * Uses JPA for persistence but exposes domain entities.
 */
@Component
public class InvoiceRepositoryImpl implements InvoiceRepository {

    private final JpaInvoiceRepository jpaRepository;
    private final InvoiceJpaMapper mapper;

    public InvoiceRepositoryImpl(JpaInvoiceRepository jpaRepository, InvoiceJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Invoice> findById(Long id) {
        return jpaRepository.findById(id)
            .map(mapper::toDomainEntity);
    }

    @Override
    public List<Invoice> findByUserId(Long userId) {
        return jpaRepository.findByUserId(userId).stream()
            .map(mapper::toDomainEntity)
            .collect(Collectors.toList());
    }

    @Override
    public Invoice save(Invoice invoice) {
        InvoiceJpaEntity jpaEntity = mapper.toJpaEntity(invoice);
        InvoiceJpaEntity savedEntity = jpaRepository.save(jpaEntity);
        return mapper.toDomainEntity(savedEntity);
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
