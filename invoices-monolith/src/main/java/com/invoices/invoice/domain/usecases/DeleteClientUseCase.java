package com.invoices.invoice.domain.usecases;

import com.invoices.invoice.domain.entities.Client;
import com.invoices.invoice.domain.ports.ClientEventPublisher;
import com.invoices.invoice.domain.ports.ClientRepository;
import com.invoices.shared.domain.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;

/**
 * UseCase for deleting a client.
 *
 * Encapsulates business logic for client deletion including:
 * - Client existence validation
 * - Event publishing before deletion
 */
@Slf4j
public class DeleteClientUseCase {

    private final ClientRepository clientRepository;
    private final ClientEventPublisher eventPublisher;

    public DeleteClientUseCase(
            ClientRepository clientRepository,
            ClientEventPublisher eventPublisher) {
        this.clientRepository = clientRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Deletes a client.
     *
     * Business logic:
     * 1. Validate client exists
     * 2. Load full client entity (for event publishing)
     * 3. Delete client
     * 4. Publish ClientDeleted event
     *
     * Note: Business rule enforcement (e.g., "cannot delete if has invoices")
     * can be added here based on requirements.
     *
     * @param clientId ID of the client to delete
     * @throws ResourceNotFoundException if client doesn't exist
     */
    public void execute(Long clientId) {
        log.debug("Deleting client {}", clientId);

        // Load client to have full entity for event
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + clientId));

        // Note: Uncomment if business rule requires validation
        // List<Invoice> invoices = invoiceRepository.findByClientId(clientId);
        // if (!invoices.isEmpty()) {
        //     throw new IllegalStateException(
        //             "Cannot delete client " + clientId +
        //             " because it has " + invoices.size() + " associated invoices");
        // }

        // Delete client
        clientRepository.deleteById(clientId);
        log.info("Client {} deleted successfully", clientId);

        // Publish event
        try {
            eventPublisher.publishClientDeleted(client);
        } catch (Exception e) {
            // Log but don't fail - deletion already happened
            log.warn("Failed to publish ClientDeleted event for client {}", clientId, e);
        }
    }
}
