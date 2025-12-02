package com.invoices.invoice.infrastructure.config;

import com.invoices.invoice.domain.ports.ClientEventPublisher;
import com.invoices.invoice.domain.ports.ClientRepository;
import com.invoices.invoice.domain.ports.CompanyRepository;
import com.invoices.invoice.domain.usecases.CreateClientUseCase;
import com.invoices.invoice.domain.usecases.DeleteClientUseCase;
import com.invoices.invoice.domain.usecases.GetAllClientsUseCase;
import com.invoices.invoice.domain.usecases.GetClientByIdUseCase;
import com.invoices.invoice.domain.usecases.UpdateClientUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for Client domain use cases.
 *
 * Provides beans for all client-related use cases.
 * Separates use case instantiation from controllers.
 * Makes use cases testable and injectable.
 */
@Configuration
public class ClientUseCaseConfiguration {

    @Bean
    public GetAllClientsUseCase getAllClientsUseCase(
            ClientRepository clientRepository,
            CompanyRepository companyRepository) {
        return new GetAllClientsUseCase(clientRepository, companyRepository);
    }

    @Bean
    public GetClientByIdUseCase getClientByIdUseCase(
            ClientRepository clientRepository) {
        return new GetClientByIdUseCase(clientRepository);
    }

    @Bean
    public CreateClientUseCase createClientUseCase(
            ClientRepository clientRepository,
            CompanyRepository companyRepository,
            ClientEventPublisher eventPublisher) {
        return new CreateClientUseCase(clientRepository, companyRepository, eventPublisher);
    }

    @Bean
    public UpdateClientUseCase updateClientUseCase(
            ClientRepository clientRepository,
            ClientEventPublisher eventPublisher) {
        return new UpdateClientUseCase(clientRepository, eventPublisher);
    }

    @Bean
    public DeleteClientUseCase deleteClientUseCase(
            ClientRepository clientRepository,
            ClientEventPublisher eventPublisher) {
        return new DeleteClientUseCase(clientRepository, eventPublisher);
    }
}
