package com.invoices.document.infrastructure.storage;

import com.invoices.document.config.MinioConfig;
import com.invoices.document.domain.entities.FileContent;
import com.invoices.document.domain.ports.FileStorageService;
import com.invoices.document.exception.FileUploadException;
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
 */
@Service
@Slf4j
public class MinioFileStorageService implements FileStorageService {

    private final MinioClient minioClient;
    private final MinioConfig.MinioProperties minioProperties;

    public MinioFileStorageService(
            MinioClient minioClient,
            MinioConfig.MinioProperties minioProperties
    ) {
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
    }

    @Override
    public void storeFile(String objectName, FileContent fileContent) {
        try {
            log.info("Storing file in MinIO: {}", objectName);

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(objectName)
                            .stream(fileContent.getInputStream(), fileContent.getSize(), -1)
                            .contentType(fileContent.getContentType())
                            .build()
            );

            log.info("File stored successfully in MinIO: {}", objectName);

        } catch (Exception e) {
            log.error("Failed to store file in MinIO: {}", objectName, e);
            throw new FileUploadException("Failed to upload file to storage", e);
        }
    }

    @Override
    public InputStream retrieveFile(String objectName) {
        try {
            log.info("Retrieving file from MinIO: {}", objectName);

            InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(objectName)
                            .build()
            );

            log.info("File retrieved successfully from MinIO: {}", objectName);
            return stream;

        } catch (Exception e) {
            log.error("Failed to retrieve file from MinIO: {}", objectName, e);
            throw new FileUploadException("Failed to download file from storage", e);
        }
    }

    @Override
    public void deleteFile(String objectName) {
        try {
            log.info("Deleting file from MinIO: {}", objectName);

            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(objectName)
                            .build()
            );

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
                            .build()
            );
            return true;

        } catch (Exception e) {
            log.debug("File does not exist in MinIO: {}", objectName);
            return false;
        }
    }
}
