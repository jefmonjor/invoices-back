package com.invoices.document.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Configuration
@Profile("!test")  // Don't load MinioConfig in test profile - mocks are provided by TestConfig
@Slf4j
public class MinioConfig {

    @Component
    @ConfigurationProperties(prefix = "s3")
    @Data
    public static class MinioProperties {
        private String endpoint;
        private String accessKey;
        private String secretKey;
        private String bucketName;
        private String region;
        private boolean pathStyleAccess;
    }

    @Bean
    public MinioClient minioClient(MinioProperties properties) {
        log.info("Initializing MinIO client with endpoint: {}", properties.getEndpoint());
        return MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
    }

    @Component
    @Slf4j
    @Profile("!test")  // Don't run in test profile
    public static class MinioInitializer {

        private final MinioClient minioClient;
        private final MinioProperties minioProperties;

        public MinioInitializer(MinioClient minioClient, MinioProperties minioProperties) {
            this.minioClient = minioClient;
            this.minioProperties = minioProperties;
        }

        @PostConstruct
        public void initBucket() {
            try {
                String bucketName = minioProperties.getBucketName();
                boolean exists = minioClient.bucketExists(
                        BucketExistsArgs.builder()
                                .bucket(bucketName)
                                .build()
                );

                if (!exists) {
                    log.info("Creating MinIO bucket: {}", bucketName);
                    minioClient.makeBucket(
                            MakeBucketArgs.builder()
                                    .bucket(bucketName)
                                    .build()
                    );
                    log.info("MinIO bucket '{}' created successfully", bucketName);
                } else {
                    log.info("MinIO bucket '{}' already exists", bucketName);
                }
            } catch (Exception e) {
                log.error("Error initializing MinIO bucket", e);
                throw new RuntimeException("Failed to initialize MinIO bucket", e);
            }
        }
    }
}
