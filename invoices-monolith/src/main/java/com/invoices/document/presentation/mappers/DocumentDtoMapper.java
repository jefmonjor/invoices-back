package com.invoices.document.presentation.mappers;

import com.invoices.document.domain.entities.Document;
import com.invoices.document.presentation.dto.DocumentDTO;
import com.invoices.document.presentation.dto.UploadDocumentResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between domain Document and presentation DTOs.
 * This isolates the domain layer from presentation concerns.
 */
@Component
public class DocumentDtoMapper {

    /**
     * Converts a domain Document entity to a DocumentDTO.
     *
     * @param domainDocument the domain document
     * @return the DocumentDTO
     */
    public DocumentDTO toDTO(Document domainDocument) {
        if (domainDocument == null) {
            return null;
        }

        return DocumentDTO.builder()
                .id(domainDocument.getId())
                .filename(domainDocument.getFilename())
                .originalFilename(domainDocument.getOriginalFilename())
                .contentType(domainDocument.getContentType())
                .fileSize(domainDocument.getFileSize())
                .invoiceId(domainDocument.getInvoiceId())
                .uploadedBy(domainDocument.getUploadedBy())
                .createdAt(domainDocument.getCreatedAt())
                .downloadUrl(generateDownloadUrl(domainDocument.getId()))
                .build();
    }

    /**
     * Converts a domain Document entity to an UploadDocumentResponse.
     *
     * @param domainDocument the domain document
     * @return the UploadDocumentResponse
     */
    public UploadDocumentResponse toUploadResponse(Document domainDocument) {
        if (domainDocument == null) {
            return null;
        }

        return UploadDocumentResponse.builder()
                .documentId(domainDocument.getId())
                .filename(domainDocument.getOriginalFilename())
                .downloadUrl(generateDownloadUrl(domainDocument.getId()))
                .uploadedAt(domainDocument.getCreatedAt())
                .build();
    }

    /**
     * Generates the download URL for a document.
     */
    private String generateDownloadUrl(Long documentId) {
        return "/api/documents/" + documentId + "/download";
    }
}
