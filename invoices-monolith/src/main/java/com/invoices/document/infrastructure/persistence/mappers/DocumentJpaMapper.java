package com.invoices.document.infrastructure.persistence.mappers;

import com.invoices.document.domain.entities.Document;
import com.invoices.document.infrastructure.persistence.entities.DocumentJpaEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between domain Document and JPA DocumentJpaEntity.
 * This isolates the domain layer from JPA/persistence concerns.
 */
@Component
public class DocumentJpaMapper {

    /**
     * Converts a domain Document entity to a JPA entity.
     *
     * @param domainDocument the domain document
     * @return the JPA entity
     */
    public DocumentJpaEntity toJpaEntity(Document domainDocument) {
        if (domainDocument == null) {
            return null;
        }

        return DocumentJpaEntity.builder()
                .id(domainDocument.getId())
                .filename(domainDocument.getFilename())
                .originalFilename(domainDocument.getOriginalFilename())
                .contentType(domainDocument.getContentType())
                .fileSize(domainDocument.getFileSize())
                .minioObjectName(domainDocument.getStorageObjectName())
                .invoiceId(domainDocument.getInvoiceId())
                .uploadedBy(domainDocument.getUploadedBy())
                .createdAt(domainDocument.getCreatedAt())
                .build();
    }

    /**
     * Converts a JPA entity to a domain Document entity.
     *
     * @param jpaEntity the JPA entity
     * @return the domain document
     */
    public Document toDomainEntity(DocumentJpaEntity jpaEntity) {
        if (jpaEntity == null) {
            return null;
        }

        return new Document(
                jpaEntity.getId(),
                jpaEntity.getFilename(),
                jpaEntity.getOriginalFilename(),
                jpaEntity.getContentType(),
                jpaEntity.getFileSize(),
                jpaEntity.getMinioObjectName(),
                jpaEntity.getInvoiceId(),
                jpaEntity.getUploadedBy(),
                jpaEntity.getCreatedAt()
        );
    }
}
