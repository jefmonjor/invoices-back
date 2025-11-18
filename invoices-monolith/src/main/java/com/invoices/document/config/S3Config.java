package com.invoices.document.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * Configuración de almacenamiento S3-compatible
 * Soporta: Cloudflare R2, MinIO, AWS S3, etc.
 */
@Configuration
@Slf4j
public class S3Config {

    @Component
    @ConfigurationProperties(prefix = "storage.s3")
    @Data
    public static class S3Properties {
        private String endpoint;
        private String accessKey;
        private String secretKey;
        private String bucketName;
        private String region;
        private boolean pathStyleAccess;
    }

    /**
     * Cliente S3 (MinIO SDK es compatible con Cloudflare R2 y AWS S3)
     */
    @Bean
    public MinioClient s3Client(S3Properties properties) {
        log.info("Initializing S3-compatible client with endpoint: {}", properties.getEndpoint());
        return MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .region(properties.getRegion())
                .build();
    }

    @Component
    @Slf4j
    public static class S3BucketInitializer {

        private final MinioClient s3Client;
        private final S3Properties s3Properties;

        public S3BucketInitializer(MinioClient s3Client, S3Properties s3Properties) {
            this.s3Client = s3Client;
            this.s3Properties = s3Properties;
        }

        @PostConstruct
        public void initBucket() {
            try {
                String bucketName = s3Properties.getBucketName();
                boolean exists = s3Client.bucketExists(
                        BucketExistsArgs.builder()
                                .bucket(bucketName)
                                .build()
                );

                if (!exists) {
                    log.info("Creating S3 bucket: {}", bucketName);
                    s3Client.makeBucket(
                            MakeBucketArgs.builder()
                                    .bucket(bucketName)
                                    .build()
                    );
                    log.info("S3 bucket '{}' created successfully", bucketName);
                } else {
                    log.info("S3 bucket '{}' already exists", bucketName);
                }
            } catch (Exception e) {
                log.warn("Could not verify/create bucket (may already exist in R2): {}", e.getMessage());
                // For Cloudflare R2, buckets are usually pre-created in the dashboard
                // So we log a warning but don't fail the application
            }
        }
    }
}
