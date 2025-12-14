package com.invoices.config;

import com.invoices.document.config.MinioConfig;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * Test configuration that excludes problematic configurations
 * that require external services (MinIO).
 */
@TestConfiguration
@ComponentScan(basePackages = "com.invoices", excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                MinioConfig.class,
                MinioConfig.MinioInitializer.class
        })
})
public class TestExcludeConfig {
    // This configuration is used to exclude problematic beans during testing
}
