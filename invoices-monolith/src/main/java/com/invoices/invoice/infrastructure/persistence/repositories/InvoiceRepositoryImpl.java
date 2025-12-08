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
    public Optional<String> findLastInvoiceNumberByCompanyAndYearWithLock(Long companyId, int year) {
        return jpaRepository.findLastInvoiceNumberByCompanyAndYearWithLock(companyId, year);
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

    @Override
    public List<InvoiceSummary> findSummariesByCompanyId(Long companyId, int page, int size) {
        return findSummariesByCompanyId(companyId, page, size, null, null);
    }

    @Override
    public List<InvoiceSummary> findSummariesByCompanyId(Long companyId, int page, int size, String search,
            String status) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC,
                        "issueDate", "id"));

        org.springframework.data.jpa.domain.Specification<InvoiceJpaEntity> spec = buildSpecification(companyId, search,
                status);

        return jpaRepository.findAll(spec, pageable).stream()
                .map(entity -> new InvoiceSummary(
                        entity.getId(),
                        entity.getInvoiceNumber(),
                        entity.getIssueDate(),
                        entity.getTotalAmount(),
                        entity.getVerifactuStatus(), // Map verifactuStatus to status in summary
                        entity.getClientId(), // Use direct column instead of lazy relation
                        entity.getCompanyId()))
                .collect(Collectors.toList());
    }

    @Override
    public long countByCompanyId(Long companyId, String search, String status) {
        org.springframework.data.jpa.domain.Specification<InvoiceJpaEntity> spec = buildSpecification(companyId, search,
                status);
        return jpaRepository.count(spec);
    }

    private org.springframework.data.jpa.domain.Specification<InvoiceJpaEntity> buildSpecification(Long companyId,
            String search, String status) {
        return (root, query, cb) -> {
            java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();

            // Company Filter (Always applied)
            predicates.add(cb.equal(root.get("companyId"), companyId));

            // Search Filter (Invoice Number or Client Name)
            if (search != null && !search.trim().isEmpty()) {
                String searchLike = "%" + search.trim().toLowerCase() + "%";
                jakarta.persistence.criteria.Predicate invoiceNumberPredicate = cb
                        .like(cb.lower(root.get("invoiceNumber")), searchLike);

                // Join with Client to search by name
                jakarta.persistence.criteria.Join<InvoiceJpaEntity, com.invoices.invoice.infrastructure.persistence.entities.ClientJpaEntity> clientJoin = root
                        .join("client", jakarta.persistence.criteria.JoinType.LEFT);
                jakarta.persistence.criteria.Predicate clientNamePredicate = cb.like(cb.lower(clientJoin.get("name")),
                        searchLike);

                predicates.add(cb.or(invoiceNumberPredicate, clientNamePredicate));
            }

            // Status Filter
            if (status != null && !status.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("verifactuStatus"), status));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }
}
