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
     * @param taxId        New tax ID / CIF (optional)
     * @param address      New address (optional)
     * @param city         New city (optional)
     * @param postalCode   New postal code (optional)
     * @param province     New province (optional)
     * @param country      New country (optional)
     * @param phone        New phone (optional)
     * @param email        New email (optional)
     * @return Updated Client entity
     * @throws ResourceNotFoundException if client doesn't exist
     * @throws IllegalArgumentException  if validation fails
     */
    public Client execute(
            Long clientId,
            String businessName,
            String taxId,
            String address,
            String city,
            String postalCode,
            String province,
            String country,
            String phone,
            String email) {
        log.debug("Updating client {}", clientId);

        // Load current client
        Client currentClient = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + clientId));

        // Validate business name if provided
        if (businessName != null && !businessName.trim().isEmpty()) {
            validateBusinessName(businessName);
        }

        // Create updated client with all fields (use current value if new value is
        // null)
        Client updatedClient = new Client(
                currentClient.getId(),
                businessName != null && !businessName.trim().isEmpty() ? businessName.trim()
                        : currentClient.getBusinessName(),
                taxId != null && !taxId.trim().isEmpty() ? taxId.trim() : currentClient.getTaxId(),
                address != null ? address.trim() : currentClient.getAddress(),
                city != null ? city.trim() : currentClient.getCity(),
                postalCode != null ? postalCode.trim() : currentClient.getPostalCode(),
                province != null ? province.trim() : currentClient.getProvince(),
                country != null ? country.trim() : currentClient.getCountry(),
                phone != null ? phone.trim() : currentClient.getPhone(),
                email != null ? email.trim() : currentClient.getEmail(),
                currentClient.getCompanyId());

        // Check if anything changed
        boolean changed = !isSameClient(currentClient, updatedClient);

        // If nothing changed, return current state
        if (!changed) {
            log.debug("No changes detected for client {}", clientId);
            return currentClient;
        }

        // Save changes
        Client savedClient = clientRepository.save(updatedClient);
        log.info("Client {} updated successfully", clientId);

        // Publish event only if changed
        try {
            eventPublisher.publishClientUpdated(savedClient);
        } catch (Exception e) {
            // Log but don't fail the transaction
            log.warn("Failed to publish ClientUpdated event for client {}", clientId, e);
        }

        return savedClient;
    }

    private boolean isSameClient(Client a, Client b) {
        return java.util.Objects.equals(a.getBusinessName(), b.getBusinessName())
                && java.util.Objects.equals(a.getTaxId(), b.getTaxId())
                && java.util.Objects.equals(a.getAddress(), b.getAddress())
                && java.util.Objects.equals(a.getCity(), b.getCity())
                && java.util.Objects.equals(a.getPostalCode(), b.getPostalCode())
                && java.util.Objects.equals(a.getProvince(), b.getProvince())
                && java.util.Objects.equals(a.getCountry(), b.getCountry())
                && java.util.Objects.equals(a.getPhone(), b.getPhone())
                && java.util.Objects.equals(a.getEmail(), b.getEmail());
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
