package com.invoices.document.infrastructure.persistence.repositories;

import com.invoices.document.infrastructure.persistence.entities.DocumentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for DocumentJpaEntity.
 * This interface provides CRUD operations and custom queries for document
 * persistence.
 * No @Repository needed - Spring Data JPA auto-detects this interface.
 */
public interface JpaDocumentRepository extends JpaRepository<DocumentJpaEntity, Long> {

    /**
     * Find all documents associated with an invoice.
     *
     * @param invoiceId the invoice ID
     * @return List of documents for the invoice
     */
    List<DocumentJpaEntity> findByInvoiceId(Long invoiceId);

    /**
     * Find a document by its storage object name (MinIO object name).
     *
     * @param minioObjectName the MinIO object name
     * @return Optional containing the document if found
     */
    Optional<DocumentJpaEntity> findByMinioObjectName(String minioObjectName);

    /**
     * Find all documents for a specific company (via invoice).
     * Uses native query because DocumentJpaEntity does not have a direct
     * relationship to InvoiceJpaEntity.
     *
     * @param companyId the company ID
     * @return List of documents for the company
     */
    @org.springframework.data.jpa.repository.Query(value = "SELECT d.* FROM documents d JOIN invoices i ON d.invoice_id = i.id WHERE i.company_id = :companyId", nativeQuery = true)
    List<DocumentJpaEntity> findByCompanyId(
            @org.springframework.data.repository.query.Param("companyId") Long companyId);
}
