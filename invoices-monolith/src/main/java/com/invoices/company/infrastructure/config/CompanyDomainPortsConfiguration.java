package com.invoices.company.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import lombok.RequiredArgsConstructor;

/**
 * Configuration for wiring domain ports to their infrastructure
 * implementations.
 * Bridges the gap between domain layer (ports) and infrastructure layer (JPA
 * repositories).
 *
 * Note: For now, this simply provides the infrastructure repositories as
 * implementations
 * of the domain ports. In a future refactoring, dedicated adapters could be
 * created
 * to handle entity conversion if domain entities differ from JPA entities.
 */
@Configuration
@RequiredArgsConstructor
public class CompanyDomainPortsConfiguration {

    // The actual implementation will be wired through specialized adapter classes
    // For now, this configuration file documents the port-to-implementation
    // mapping:
    //
    // Domain Port -> Infrastructure Implementation
    // UserCompanyRepository (port) -> UserCompanyRepository (JPA)
    // CompanyInvitationRepository (port) -> CompanyInvitationRepository (JPA)
    //
    // To complete this mapping at runtime, adapters should be created that
    // implement
    // the domain ports and delegate to the JPA repositories.
}
