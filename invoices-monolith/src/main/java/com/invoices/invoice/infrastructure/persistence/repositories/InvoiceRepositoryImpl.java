package com.invoices.invoice.infrastructure.persistence.repositories;

import com.invoices.invoice.domain.entities.Invoice;
import com.invoices.invoice.domain.models.InvoiceSummary;
import com.invoices.invoice.domain.ports.InvoiceRepository;
import com.invoices.invoice.infrastructure.persistence.entities.InvoiceJpaEntity;
import com.invoices.invoice.infrastructure.persistence.mappers.InvoiceJpaMapper;

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
    public List<Invoice> findAll() {
        return jpaRepository.findAll().stream()
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
    public void delete(Invoice invoice) {
        InvoiceJpaEntity jpaEntity = mapper.toJpaEntity(invoice);
        jpaRepository.delete(jpaEntity);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return jpaRepository.existsById(id);
    }

    @Override
    public Optional<String> findLastInvoiceNumberByYear(int year) {
        return jpaRepository.findLastInvoiceNumberByYear(year);
    }

    @Override
    public Optional<String> findLastInvoiceNumberByCompanyAndYear(Long companyId, int year) {
        return jpaRepository.findLastInvoiceNumberByCompanyAndYear(companyId, year);
    }

    @Override
    public List<Invoice> findByCompanyId(Long companyId) {
        return jpaRepository.findByCompanyId(companyId).stream()
                .map(mapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public long countByCompanyId(Long companyId) {
        return jpaRepository.countByCompanyId(companyId);
    }

    @Override
    public void deleteByCompanyId(Long companyId) {
        jpaRepository.deleteByCompanyId(companyId);
    }

    @Override
    public Optional<Invoice> findLastInvoiceByCompanyIdAndIdNot(Long companyId, Long excludedInvoiceId) {
        return jpaRepository.findFirstByCompanyIdAndIdNotOrderByCreatedAtDesc(companyId, excludedInvoiceId)
                .map(mapper::toDomainEntity);
    }

    @Override
    public List<InvoiceSummary> findSummariesByCompanyId(Long companyId) {
        return jpaRepository.findProjectedByCompanyId(companyId).stream()
                .map(view -> new InvoiceSummary(
                        view.getId(),
                        view.getInvoiceNumber(),
                        view.getIssueDate(),
                        view.getTotalAmount(),
                        view.getStatus(),
                        view.getClientId(),
                        view.getCompanyId()))
                .collect(Collectors.toList());
    }
}
