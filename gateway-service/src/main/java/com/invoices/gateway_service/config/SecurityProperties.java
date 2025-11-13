package com.invoices.gateway_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for security settings.
 * Loads public paths from application.yml.
 */
@Component
@ConfigurationProperties(prefix = "security")
@Getter
@Setter
public class SecurityProperties {

    /**
     * List of public paths that don't require JWT authentication.
     */
    private List<String> publicPaths = new ArrayList<>();
}
