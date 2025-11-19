package com.invoices.document.domain.usecases;

import com.invoices.document.domain.entities.Document;
import com.invoices.document.domain.ports.DocumentRepository;
import com.invoices.document.exception.DocumentNotFoundException;

/**
 * Use Case for retrieving a document by ID.
 * Returns the document metadata without file content.
 */
public class GetDocumentByIdUseCase {

    private final DocumentRepository documentRepository;

    public GetDocumentByIdUseCase(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    /**
     * Executes the get document by ID use case.
     *
     * @param documentId the ID of the document to retrieve
     * @return the Document entity
     * @throws DocumentNotFoundException if document not found
     */
    public Document execute(Long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));
    }
}
