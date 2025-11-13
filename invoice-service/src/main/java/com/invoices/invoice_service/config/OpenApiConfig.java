package com.invoices.invoice_service.config;

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
 * Configuración de OpenAPI/Swagger para el servicio de facturas
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8082}")
    private String serverPort;

    @Bean
    public OpenAPI invoiceServiceOpenAPI() {
        Server localServer = new Server();
        localServer.setUrl("http://localhost:" + serverPort);
        localServer.setDescription("Servidor local de desarrollo");

        Server gatewayServer = new Server();
        gatewayServer.setUrl("http://localhost:8080");
        gatewayServer.setDescription("API Gateway");

        Contact contact = new Contact();
        contact.setEmail("invoices@example.com");
        contact.setName("Invoice Service Team");

        License license = new License()
                .name("MIT License")
                .url("https://opensource.org/licenses/MIT");

        Info info = new Info()
                .title("Invoice Service API")
                .version("1.0.0")
                .contact(contact)
                .description("API REST para gestionar facturas, items y generar PDFs con JasperReports")
                .license(license);

        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer, gatewayServer));
    }
}
