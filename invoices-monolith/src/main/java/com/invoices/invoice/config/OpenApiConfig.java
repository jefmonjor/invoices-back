package com.invoices.invoice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuración de OpenAPI/Swagger para la aplicación monolítica
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI invoiceServiceOpenAPI() {
        Server localServer = new Server();
        localServer.setUrl("http://localhost:" + serverPort);
        localServer.setDescription("Servidor local de desarrollo");

        Server productionServer = new Server();
        productionServer.setUrl("https://invoices-app.fly.dev");
        productionServer.setDescription("Servidor de producción (Fly.io)");

        Contact contact = new Contact();
        contact.setEmail("invoices@example.com");
        contact.setName("Invoices Team");

        License license = new License()
                .name("MIT License")
                .url("https://opensource.org/licenses/MIT");

        Info info = new Info()
                .title("Invoices Monolith API")
                .version("1.0.0")
                .contact(contact)
                .description("API REST monolítica para gestionar usuarios, facturas, documentos y auditoría")
                .license(license);

        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer, productionServer));
    }
}
