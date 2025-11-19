package com.invoices.document.domain.ports;

import com.invoices.document.domain.entities.FileContent;

import java.io.InputStream;

/**
 * Port (interface) for file storage operations.
 * This abstracts the underlying storage implementation (MinIO, S3, filesystem, etc.).
 * Infrastructure layer will provide the actual implementation.
 */
public interface FileStorageService {

    /**
     * Store a file in the storage system.
     *
     * @param objectName  the unique name for the stored object
     * @param fileContent the file content to store
     * @throws RuntimeException if storage operation fails
     */
    void storeFile(String objectName, FileContent fileContent);

    /**
     * Retrieve a file from the storage system.
     *
     * @param objectName the unique name of the stored object
     * @return InputStream of the file content
     * @throws RuntimeException if file not found or retrieval fails
     */
    InputStream retrieveFile(String objectName);

    /**
     * Delete a file from the storage system.
     *
     * @param objectName the unique name of the stored object
     * @throws RuntimeException if deletion fails
     */
    void deleteFile(String objectName);

    /**
     * Check if a file exists in the storage system.
     *
     * @param objectName the unique name of the stored object
     * @return true if file exists, false otherwise
     */
    boolean fileExists(String objectName);
}
