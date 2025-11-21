package com.invoices.document.infrastructure.storage;

import com.invoices.document.config.MinioConfig;
import com.invoices.document.domain.entities.FileContent;
import com.invoices.document.domain.ports.FileStorageService;
import com.invoices.document.exception.FileUploadException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * Implementation of FileStorageService port using MinIO.
 * This adapter connects the domain layer with MinIO storage infrastructure.
 *
 * Circuit breaker is configured to protect against MinIO service failures.
 * When the circuit is open, fallback methods will be called.
 */
@Service
@Slf4j
public class MinioFileStorageService implements FileStorageService {

    private final MinioClient minioClient;
    private final MinioConfig.MinioProperties minioProperties;

    public MinioFileStorageService(
            MinioClient minioClient,
            MinioConfig.MinioProperties minioProperties) {
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
    }

    @Override
    @CircuitBreaker(name = "minio", fallbackMethod = "storeFileFallback")
    public void storeFile(String objectName, FileContent fileContent) {
        try {
            log.info("Storing file in MinIO: {}", objectName);

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(objectName)
                            .stream(fileContent.getInputStream(), fileContent.getSize(), -1)
                            .contentType(fileContent.getContentType())
                            .build());

            log.info("File stored successfully in MinIO: {}", objectName);

        } catch (Exception e) {
            log.error("Failed to store file in MinIO: {}", objectName, e);
            throw new FileUploadException("Failed to upload file to storage", e);
        }
    }

    /**
     * Fallback method for storeFile when MinIO circuit is open.
     */
    @SuppressWarnings("unused") // Called by CircuitBreaker via reflection
    private void storeFileFallback(String objectName, FileContent fileContent, Exception e) {
        log.error("Circuit breaker activated for file storage. MinIO service is unavailable", e);
        throw new FileUploadException("Storage service temporarily unavailable. Please try again later.", e);
    }

    @Override
    @CircuitBreaker(name = "minio")
    public InputStream retrieveFile(String objectName) {
        try {
            log.info("Retrieving file from MinIO: {}", objectName);

            InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(objectName)
                            .build());

            log.info("File retrieved successfully from MinIO: {}", objectName);
            return stream;

        } catch (Exception e) {
            log.error("Failed to retrieve file from MinIO: {}", objectName, e);
            throw new FileUploadException("Failed to download file from storage", e);
        }
    }

    @Override
    @CircuitBreaker(name = "minio")
    public void deleteFile(String objectName) {
        try {
            log.info("Deleting file from MinIO: {}", objectName);

            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(objectName)
                            .build());

            log.info("File deleted successfully from MinIO: {}", objectName);

        } catch (Exception e) {
            log.error("Failed to delete file from MinIO: {}", objectName, e);
            throw new FileUploadException("Failed to delete file from storage", e);
        }
    }

    @Override
    public boolean fileExists(String objectName) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(objectName)
                            .build());
            return true;

        } catch (Exception e) {
            log.debug("File does not exist in MinIO: {}", objectName);
            return false;
        }
    }
}
