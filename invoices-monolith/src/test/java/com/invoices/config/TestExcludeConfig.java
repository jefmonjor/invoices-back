package com.invoices.config;

import com.invoices.document.config.MinioConfig;
import com.invoices.document.config.S3Config;
import com.invoices.trace.config.RedisStreamConfig;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * Test configuration that excludes problematic configurations
 * that require external services (MinIO, S3, Redis Streams).
 */
@TestConfiguration
@ComponentScan(
    basePackages = "com.invoices",
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
            MinioConfig.class,
            MinioConfig.MinioInitializer.class,
            S3Config.class,
            S3Config.S3BucketInitializer.class,
            RedisStreamConfig.class
        })
    }
)
public class TestExcludeConfig {
    // This configuration is used to exclude problematic beans during testing
}
