package com.invoices.document.domain.ports;

import com.invoices.document.domain.entities.Document;

import java.util.List;
import java.util.Optional;

/**
 * Port (interface) for document repository operations.
 * This defines the contract for persistence without exposing implementation
 * details.
 * Infrastructure layer will provide the actual implementation.
 */
public interface DocumentRepository {

    /**
     * Find a document by its ID.
     *
     * @param id the document ID
     * @return Optional containing the document if found
     */
    Optional<Document> findById(Long id);

    /**
     * Find all documents associated with an invoice.
     *
     * @param invoiceId the invoice ID
     * @return List of documents for the invoice
     */
    List<Document> findByInvoiceId(Long invoiceId);

    /**
     * Find a document by its storage object name.
     *
     * @param storageObjectName the storage object name (e.g., MinIO object name)
     * @return Optional containing the document if found
     */
    Optional<Document> findByStorageObjectName(String storageObjectName);

    /**
     * Save a document (create or update).
     *
     * @param document the document to save
     * @return the saved document with generated ID if new
     */
    Document save(Document document);

    /**
     * Delete a document by ID.
     *
     * @param id the document ID
     */
    void deleteById(Long id);

    /**
     * Check if a document exists by ID.
     *
     * @param id the document ID
     * @return true if exists, false otherwise
     */
    boolean existsById(Long id);

    /**
     * Find all documents for a specific company (via invoice).
     *
     * @param companyId the company ID
     * @return List of documents for the company
     */
    List<Document> findByCompanyId(Long companyId);
}
