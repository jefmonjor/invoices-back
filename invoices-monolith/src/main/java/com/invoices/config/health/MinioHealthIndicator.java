package com.invoices.config.health;

import io.minio.MinioClient;
import io.minio.BucketExistsArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health indicator for MinIO storage service.
 * Checks if the configured bucket is accessible.
 */
@Component("minio")
@RequiredArgsConstructor
@Slf4j
public class MinioHealthIndicator implements HealthIndicator {

    private final MinioClient minioClient;
    private final com.invoices.document.config.MinioConfig.MinioProperties minioProperties;

    @Override
    public Health health() {
        try {
            String bucketName = minioProperties.getBucketName();
            boolean bucketExists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucketName)
                            .build());

            if (bucketExists) {
                return Health.up()
                        .withDetail("bucket", bucketName)
                        .withDetail("endpoint", minioProperties.getEndpoint())
                        .build();
            } else {
                return Health.down()
                        .withDetail("bucket", bucketName)
                        .withDetail("error", "Bucket does not exist")
                        .build();
            }

        } catch (Exception e) {
            log.error("MinIO health check failed", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
