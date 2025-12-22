package com.invoices.document.domain.services;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Service to resolve storage object names to presigned URLs.
 * Used for displaying logos and other assets with time-limited access.
 */
@Service
@Slf4j
public class StorageUrlResolver {

    private final MinioClient minioClient;

    @Value("${s3.bucket-name}")
    private String bucketName;

    @Value("${s3.presigned-url-expiry-hours:24}")
    private int presignedUrlExpiryHours;

    public StorageUrlResolver(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    /**
     * Convert an object name (storage key) to a presigned URL.
     * The URL will be valid for a limited time (default: 24 hours).
     * 
     * @param objectName The object name in storage (e.g.,
     *                   "logos/company-7-xxx.png")
     * @return A presigned URL that grants temporary read access
     */
    public String resolvePublicUrl(String objectName) {
        if (objectName == null || objectName.isEmpty()) {
            return null;
        }

        // If it's already a full URL, return as-is
        if (objectName.startsWith("http://") || objectName.startsWith("https://")) {
            return objectName;
        }

        try {
            String presignedUrl = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(presignedUrlExpiryHours, TimeUnit.HOURS)
                            .build());

            log.debug("Generated presigned URL for object: {} (expires in {} hours)", objectName,
                    presignedUrlExpiryHours);
            return presignedUrl;
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for object: {}", objectName, e);
            return null;
        }
    }
}
