package com.invoices.document.domain.usecases;

import com.invoices.document.domain.entities.Document;
import com.invoices.document.domain.ports.DocumentRepository;
import com.invoices.document.domain.ports.FileStorageService;
import com.invoices.document.exception.DocumentNotFoundException;

import java.io.InputStream;

/**
 * Use Case for downloading a document.
 * Retrieves the document content from storage.
 */
public class DownloadDocumentUseCase {

    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;

    public DownloadDocumentUseCase(
            DocumentRepository documentRepository,
            FileStorageService fileStorageService
    ) {
        this.documentRepository = documentRepository;
        this.fileStorageService = fileStorageService;
    }

    /**
     * Executes the download document use case.
     *
     * @param documentId the ID of the document to download
     * @return InputStream of the document content
     * @throws DocumentNotFoundException if document not found
     */
    public InputStream execute(Long documentId) {
        // Find document metadata
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));

        // Retrieve file from storage
        return fileStorageService.retrieveFile(document.getStorageObjectName());
    }

    /**
     * Gets the document metadata for a given ID.
     * This is useful when the caller needs both content and metadata.
     *
     * @param documentId the ID of the document
     * @return the Document entity
     * @throws DocumentNotFoundException if document not found
     */
    public Document getDocumentMetadata(Long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));
    }
}
