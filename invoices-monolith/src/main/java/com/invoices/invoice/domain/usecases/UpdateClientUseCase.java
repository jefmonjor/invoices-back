package com.invoices.invoice.domain.usecases;

import com.invoices.invoice.domain.entities.Client;
import com.invoices.invoice.domain.ports.ClientEventPublisher;
import com.invoices.invoice.domain.ports.ClientRepository;
import com.invoices.shared.domain.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;

/**
 * UseCase for updating an existing client.
 *
 * Encapsulates business logic for client updates including:
 * - Client existence validation
 * - Data validation
 * - Change tracking
 * - Event publishing only if changed
 */
@Slf4j
public class UpdateClientUseCase {

    private final ClientRepository clientRepository;
    private final ClientEventPublisher eventPublisher;

    public UpdateClientUseCase(
            ClientRepository clientRepository,
            ClientEventPublisher eventPublisher) {
        this.clientRepository = clientRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Updates an existing client.
     *
     * Business logic:
     * 1. Load current client
     * 2. Apply changes if values are provided
     * 3. Validate updated data
     * 4. Save changes if anything changed
     * 5. Publish ClientUpdated event only if changed
     *
     * @param clientId     ID of the client to update
     * @param businessName New business name (optional)
     * @param email        New email (optional)
     * @return Updated Client entity
     * @throws ResourceNotFoundException if client doesn't exist
     * @throws IllegalArgumentException  if validation fails
     */
    public Client execute(Long clientId, String businessName, String email) {
        log.debug("Updating client {}", clientId);

        // Load current client
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + clientId));

        // Track if anything changes
        boolean changed = false;

        // Update business name if provided
        if (businessName != null && !businessName.trim().isEmpty()) {
            validateBusinessName(businessName);
            if (!client.getBusinessName().equals(businessName.trim())) {
                log.debug("Updating business name for client {}: {} -> {}",
                        clientId, client.getBusinessName(), businessName);
                client = client.withBusinessName(businessName.trim());
                changed = true;
            }
        }

        // Update email if provided
        if (email != null && !email.trim().isEmpty()) {
            String newEmail = email.trim();
            if (client.getEmail() == null || !client.getEmail().equals(newEmail)) {
                log.debug("Updating email for client {}: {} -> {}",
                        clientId, client.getEmail(), email);
                client = client.withEmail(newEmail);
                changed = true;
            }
        }

        // If nothing changed, return current state
        if (!changed) {
            log.debug("No changes detected for client {}", clientId);
            return client;
        }

        // Save changes
        Client updatedClient = clientRepository.save(client);
        log.info("Client {} updated successfully", clientId);

        // Publish event only if changed
        try {
            eventPublisher.publishClientUpdated(updatedClient);
        } catch (Exception e) {
            // Log but don't fail the transaction
            log.warn("Failed to publish ClientUpdated event for client {}", clientId, e);
        }

        return updatedClient;
    }

    /**
     * Validates business name.
     *
     * @param businessName Business name to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateBusinessName(String businessName) {
        if (businessName == null || businessName.trim().isEmpty()) {
            throw new IllegalArgumentException("Business name cannot be empty");
        }

        if (businessName.trim().length() > 255) {
            throw new IllegalArgumentException("Business name cannot exceed 255 characters");
        }
    }
}
