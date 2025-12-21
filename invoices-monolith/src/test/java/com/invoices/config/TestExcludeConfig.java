package com.invoices.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@TestConfiguration
@ComponentScan(basePackages = "com.invoices", excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                                com.invoices.document.config.MinioConfig.class,
                                com.invoices.document.config.MinioConfig.MinioInitializer.class
                })
})
public class TestExcludeConfig {
        // This configuration is used to exclude problematic beans during testing
}
