package com.invoices.document.domain.entities;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain entity representing a document (Clean Architecture - Pure POJO).
 * This entity contains only business logic and has no framework dependencies.
 */
public class Document {

    private final Long id;
    private final String filename;
    private final String originalFilename;
    private final String contentType;
    private final Long fileSize;
    private final String storageObjectName;
    private final Long invoiceId;
    private final String uploadedBy;
    private final LocalDateTime createdAt;

    /**
     * Full constructor for creating a Document domain entity.
     */
    public Document(
            Long id,
            String filename,
            String originalFilename,
            String contentType,
            Long fileSize,
            String storageObjectName,
            Long invoiceId,
            String uploadedBy,
            LocalDateTime createdAt
    ) {
        validateRequiredFields(filename, originalFilename, contentType, fileSize, storageObjectName);

        this.id = id;
        this.filename = filename;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.storageObjectName = storageObjectName;
        this.invoiceId = invoiceId;
        this.uploadedBy = uploadedBy;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    /**
     * Constructor for new documents (before persistence - no ID).
     */
    public Document(
            String filename,
            String originalFilename,
            String contentType,
            Long fileSize,
            String storageObjectName,
            Long invoiceId,
            String uploadedBy
    ) {
        this(null, filename, originalFilename, contentType, fileSize,
             storageObjectName, invoiceId, uploadedBy, LocalDateTime.now());
    }

    /**
     * Validates required fields for the document.
     */
    private void validateRequiredFields(String filename, String originalFilename,
                                       String contentType, Long fileSize, String storageObjectName) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be null or empty");
        }
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new IllegalArgumentException("Original filename cannot be null or empty");
        }
        if (contentType == null || contentType.trim().isEmpty()) {
            throw new IllegalArgumentException("Content type cannot be null or empty");
        }
        if (fileSize == null || fileSize <= 0) {
            throw new IllegalArgumentException("File size must be greater than zero");
        }
        if (storageObjectName == null || storageObjectName.trim().isEmpty()) {
            throw new IllegalArgumentException("Storage object name cannot be null or empty");
        }
    }

    // Business methods

    /**
     * Checks if this document is a PDF file.
     */
    public boolean isPdf() {
        return "application/pdf".equals(contentType);
    }

    /**
     * Checks if this document is associated with an invoice.
     */
    public boolean hasInvoice() {
        return invoiceId != null;
    }

    /**
     * Gets the file size in megabytes.
     */
    public double getFileSizeInMB() {
        return fileSize / (1024.0 * 1024.0);
    }

    /**
     * Checks if file size exceeds a given limit in bytes.
     */
    public boolean exceedsSizeLimit(long maxSizeInBytes) {
        return fileSize > maxSizeInBytes;
    }

    /**
     * Gets the file extension from the original filename.
     */
    public String getFileExtension() {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf("."));
    }

    // Getters

    public Long getId() {
        return id;
    }

    public String getFilename() {
        return filename;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public String getStorageObjectName() {
        return storageObjectName;
    }

    public Long getInvoiceId() {
        return invoiceId;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Document document = (Document) o;
        return Objects.equals(id, document.id) &&
               Objects.equals(storageObjectName, document.storageObjectName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, storageObjectName);
    }

    @Override
    public String toString() {
        return "Document{" +
                "id=" + id +
                ", filename='" + filename + '\'' +
                ", originalFilename='" + originalFilename + '\'' +
                ", contentType='" + contentType + '\'' +
                ", fileSize=" + fileSize +
                ", storageObjectName='" + storageObjectName + '\'' +
                ", invoiceId=" + invoiceId +
                ", uploadedBy='" + uploadedBy + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
