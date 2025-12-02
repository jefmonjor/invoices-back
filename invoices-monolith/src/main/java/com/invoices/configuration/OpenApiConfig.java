package com.invoices.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for multi-company invoice API
 * 
 * Access Swagger UI at: /swagger-ui.html
 * Access API docs at: /v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:Multi-Company Invoice API}")
    private String applicationName;

    @Value("${spring.application.version:2.0.0}")
    private String applicationVersion;

    @Value("${openapi.servers.development:http://localhost:8080}")
    private String developmentServerUrl;

    @Value("${openapi.servers.production:https://api.invoices.com}")
    private String productionServerUrl;

    @Value("${openapi.servers.staging:}")
    private String stagingServerUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(serverList())
                .addSecurityItem(securityRequirement())
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("bearer-jwt", securityScheme()));
    }

    private Info apiInfo() {
        return new Info()
                .title(applicationName)
                .version(applicationVersion)
                .description("""
                        ## Multi-Company Invoice Management API

                        Complete REST API for managing invoices, clients, and companies with role-based access control.

                        ### Features
                        - 🏢 Multi-company support
                        - 🔐 Role-based permissions (ADMIN/USER)
                        - 📄 Invoice CRUD operations
                        - 👥 Client management
                        - 🎫 Company invitation system
                        - 📊 Audit logging

                        ### Authentication
                        All endpoints (except public ones) require JWT Bearer token authentication.

                        ### Permission Matrix
                        | Operation | ADMIN | USER |
                        |-----------|-------|------|
                        | View Invoices/Clients | ✓ | ✓ |
                        | Create Invoice/Client | ✓ | ✓ |
                        | Edit Invoice | ✓ | ✗ |
                        | Delete Invoice | ✓ | ✗ |
                        | Delete Client | ✓ | ✗ |
                        | Manage Companies | ✓ | ✗ |
                        | Manage Users | ✓ | ✗ |

                        ### Getting Started
                        1. Authenticate via `/api/auth/login` to get JWT token
                        2. Include token in `Authorization: Bearer {token}` header
                        3. Use endpoints according to your role permissions
                        """)
                .contact(new Contact()
                        .name("API Support")
                        .email("support@invoices.com")
                        .url("https://invoices.com/support"))
                .license(new License()
                        .name("Private")
                        .url("https://invoices.com/license"));
    }

    private List<Server> serverList() {
        List<Server> servers = new java.util.ArrayList<>();

        // Development server
        if (developmentServerUrl != null && !developmentServerUrl.isEmpty()) {
            servers.add(new Server()
                    .url(developmentServerUrl)
                    .description("Local Development Server"));
        }

        // Staging server (if configured)
        if (stagingServerUrl != null && !stagingServerUrl.isEmpty()) {
            servers.add(new Server()
                    .url(stagingServerUrl)
                    .description("Staging Server"));
        }

        // Production server
        if (productionServerUrl != null && !productionServerUrl.isEmpty()) {
            servers.add(new Server()
                    .url(productionServerUrl)
                    .description("Production Server"));
        }

        return servers;
    }

    private SecurityRequirement securityRequirement() {
        return new SecurityRequirement().addList("bearer-jwt");
    }

    private SecurityScheme securityScheme() {
        return new SecurityScheme()
                .name("bearer-jwt")
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("""
                        JWT Bearer token authentication.

                        To get a token:
                        1. POST /api/auth/login with credentials
                        2. Copy the `accessToken` from response
                        3. Click 'Authorize' button above
                        4. Enter: `Bearer {your-token}`
                        """);
    }
}
