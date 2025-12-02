package com.invoices.invoice.domain.usecases;

import com.invoices.invoice.domain.entities.Client;
import com.invoices.invoice.domain.ports.ClientRepository;
import com.invoices.shared.domain.exception.ResourceNotFoundException;

/**
 * UseCase for retrieving a specific client by ID.
 *
 * Encapsulates the business logic for accessing a client.
 * Validates that the client exists before returning.
 */
public class GetClientByIdUseCase {

    private final ClientRepository clientRepository;

    public GetClientByIdUseCase(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    /**
     * Retrieves a specific client by ID.
     *
     * @param clientId ID of the client to retrieve
     * @return Client entity
     * @throws ResourceNotFoundException if client doesn't exist
     * @throws IllegalArgumentException if clientId is null
     */
    public Client execute(Long clientId) {
        if (clientId == null) {
            throw new IllegalArgumentException("Client ID cannot be null");
        }

        return clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + clientId));
    }
}
