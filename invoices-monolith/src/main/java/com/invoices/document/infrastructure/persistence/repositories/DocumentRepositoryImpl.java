package com.invoices.document.infrastructure.persistence.repositories;

import com.invoices.document.domain.entities.Document;
import com.invoices.document.domain.ports.DocumentRepository;
import com.invoices.document.infrastructure.persistence.entities.DocumentJpaEntity;
import com.invoices.document.infrastructure.persistence.mappers.DocumentJpaMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of DocumentRepository port using JPA.
 * This adapter connects the domain layer with Spring Data JPA infrastructure.
 */
@Repository
public class DocumentRepositoryImpl implements DocumentRepository {

    private final JpaDocumentRepository jpaDocumentRepository;
    private final DocumentJpaMapper mapper;

    public DocumentRepositoryImpl(JpaDocumentRepository jpaDocumentRepository, DocumentJpaMapper mapper) {
        this.jpaDocumentRepository = jpaDocumentRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Document> findById(Long id) {
        return jpaDocumentRepository.findById(id)
                .map(mapper::toDomainEntity);
    }

    @Override
    public List<Document> findByInvoiceId(Long invoiceId) {
        return jpaDocumentRepository.findByInvoiceId(invoiceId).stream()
                .map(mapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Document> findByStorageObjectName(String storageObjectName) {
        return jpaDocumentRepository.findByMinioObjectName(storageObjectName)
                .map(mapper::toDomainEntity);
    }

    @Override
    public Document save(Document document) {
        DocumentJpaEntity jpaEntity = mapper.toJpaEntity(document);
        DocumentJpaEntity savedEntity = jpaDocumentRepository.save(jpaEntity);
        return mapper.toDomainEntity(savedEntity);
    }

    @Override
    public void deleteById(Long id) {
        jpaDocumentRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return jpaDocumentRepository.existsById(id);
    }

    @Override
    public List<Document> findByCompanyId(Long companyId) {
        return jpaDocumentRepository.findByCompanyId(companyId).stream()
                .map(mapper::toDomainEntity)
                .collect(Collectors.toList());
    }
}
