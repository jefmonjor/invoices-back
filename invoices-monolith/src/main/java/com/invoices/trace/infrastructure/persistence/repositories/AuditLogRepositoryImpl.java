package com.invoices.trace.infrastructure.persistence.repositories;

import com.invoices.trace.domain.entities.AuditLog;
import com.invoices.trace.domain.ports.AuditLogRepository;
import com.invoices.trace.infrastructure.persistence.entities.AuditLogJpaEntity;
import com.invoices.trace.infrastructure.persistence.mappers.AuditLogJpaMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of AuditLogRepository port using JPA.
 * This adapter connects the domain layer with Spring Data JPA infrastructure.
 */
@Repository
public class AuditLogRepositoryImpl implements AuditLogRepository {

    private final JpaAuditLogRepository jpaAuditLogRepository;
    private final AuditLogJpaMapper mapper;

    public AuditLogRepositoryImpl(JpaAuditLogRepository jpaAuditLogRepository, AuditLogJpaMapper mapper) {
        this.jpaAuditLogRepository = jpaAuditLogRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<AuditLog> findById(Long id) {
        return jpaAuditLogRepository.findById(id)
                .map(mapper::toDomainEntity);
    }

    @Override
    public List<AuditLog> findByInvoiceId(Long invoiceId) {
        return jpaAuditLogRepository.findByInvoiceIdOrderByCreatedAtDesc(invoiceId).stream()
                .map(mapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<AuditLog> findByClientId(Long clientId) {
        return jpaAuditLogRepository.findByClientIdOrderByCreatedAtDesc(clientId).stream()
                .map(mapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<AuditLog> findByEventType(String eventType) {
        return jpaAuditLogRepository.findByEventTypeOrderByCreatedAtDesc(eventType).stream()
                .map(mapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public Page<AuditLog> findAll(Pageable pageable) {
        return jpaAuditLogRepository.findAll(pageable)
                .map(mapper::toDomainEntity);
    }

    @Override
    public AuditLog save(AuditLog auditLog) {
        AuditLogJpaEntity jpaEntity = mapper.toJpaEntity(auditLog);
        AuditLogJpaEntity savedEntity = jpaAuditLogRepository.save(jpaEntity);
        return mapper.toDomainEntity(savedEntity);
    }

    @Override
    public void deleteById(Long id) {
        jpaAuditLogRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return jpaAuditLogRepository.existsById(id);
    }
}
