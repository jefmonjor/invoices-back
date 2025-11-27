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
        return List.of(
                new Server()
                        .url("http://localhost:8080")
                        .description("Local Development Server"),
                new Server()
                        .url("https://api.invoices.com")
                        .description("Production Server (if applicable)"));
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
