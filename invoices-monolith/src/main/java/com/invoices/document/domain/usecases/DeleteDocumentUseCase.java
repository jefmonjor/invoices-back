package com.invoices.document.domain.usecases;

import com.invoices.document.domain.entities.Document;
import com.invoices.document.domain.ports.DocumentRepository;
import com.invoices.document.domain.ports.FileStorageService;
import com.invoices.document.exception.DocumentNotFoundException;

/**
 * Use Case for deleting a document.
 * Deletes both the file from storage and the metadata from the database.
 */
public class DeleteDocumentUseCase {

    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;

    public DeleteDocumentUseCase(
            DocumentRepository documentRepository,
            FileStorageService fileStorageService
    ) {
        this.documentRepository = documentRepository;
        this.fileStorageService = fileStorageService;
    }

    /**
     * Executes the delete document use case.
     *
     * @param documentId the ID of the document to delete
     * @throws DocumentNotFoundException if document not found
     */
    public void execute(Long documentId) {
        // Find document metadata
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));

        // Delete file from storage
        fileStorageService.deleteFile(document.getStorageObjectName());

        // Delete metadata from repository
        documentRepository.deleteById(documentId);
    }
}
