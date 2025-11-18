package com.invoices.document.domain.usecases;

import com.invoices.document.domain.entities.Document;
import com.invoices.document.domain.ports.DocumentRepository;

import java.util.List;

/**
 * Use Case for retrieving all documents associated with an invoice.
 */
public class GetDocumentsByInvoiceUseCase {

    private final DocumentRepository documentRepository;

    public GetDocumentsByInvoiceUseCase(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    /**
     * Executes the get documents by invoice use case.
     *
     * @param invoiceId the ID of the invoice
     * @return List of documents associated with the invoice
     */
    public List<Document> execute(Long invoiceId) {
        if (invoiceId == null) {
            throw new IllegalArgumentException("Invoice ID cannot be null");
        }
        return documentRepository.findByInvoiceId(invoiceId);
    }
}
