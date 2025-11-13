package com.invoices.document_service.exception;

public class DocumentNotFoundException extends RuntimeException {
    public DocumentNotFoundException(String message) {
        super(message);
    }

    public DocumentNotFoundException(Long documentId) {
        super("Document not found with ID: " + documentId);
    }
}
