package com.invoices.document.infrastructure.persistence.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * JPA Entity for document persistence.
 * This is the infrastructure representation of a Document,
 * separate from the domain entity to maintain clean architecture.
 */
@Entity
@Table(name = "documents", indexes = {
        @Index(name = "idx_document_invoice_id", columnList = "invoice_id"),
        @Index(name = "idx_document_storage_object", columnList = "minio_object_name")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String filename;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "minio_object_name", nullable = false, unique = true, length = 255)
    private String minioObjectName;

    @Column(name = "invoice_id")
    private Long invoiceId;

    @Column(name = "uploaded_by", length = 255)
    private String uploadedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
