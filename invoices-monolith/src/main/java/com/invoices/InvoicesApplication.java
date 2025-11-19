package com.invoices;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for the Invoices Monolith
 *
 * This monolithic application consolidates all microservices:
 * - User Service: User management and authentication
 * - Invoice Service: Invoice and item management
 * - Document Service: PDF generation and document storage
 * - Trace Service: Audit logging and traceability
 *
 * All services are now integrated into a single deployable unit
 * for simplified deployment and operation on Fly.io
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableAsync
@EnableScheduling
public class InvoicesApplication {

	public static void main(String[] args) {
		SpringApplication.run(InvoicesApplication.class, args);
	}

}
