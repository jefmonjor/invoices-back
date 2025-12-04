package com.invoices.invoice.presentation.controllers;

import com.invoices.invoice.domain.entities.Client;
import com.invoices.invoice.domain.usecases.CreateClientUseCase;
import com.invoices.invoice.domain.usecases.DeleteClientUseCase;
import com.invoices.invoice.domain.usecases.GetAllClientsUseCase;
import com.invoices.invoice.domain.usecases.GetClientByIdUseCase;
import com.invoices.invoice.domain.usecases.UpdateClientUseCase;
import com.invoices.invoice.dto.ClientDTO;
import com.invoices.invoice.presentation.mappers.ClientDtoMapper;
import com.invoices.security.context.CompanyContext;
import com.invoices.shared.domain.exception.ResourceNotFoundException;
import com.invoices.shared.infrastructure.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for Client operations.
 *
 * Delegates all business logic to domain use cases.
 * Responsible only for:
 * - Extracting HTTP request parameters
 * - Calling appropriate use case
 * - Mapping domain entities to DTOs
 * - Returning HTTP responses
 *
 * No business logic should be in this controller.
 */
@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Clients", description = "Endpoints for client management")
public class ClientController {

    // Inject use cases instead of repositories
    private final GetAllClientsUseCase getAllClientsUseCase;
    private final GetClientByIdUseCase getClientByIdUseCase;
    private final CreateClientUseCase createClientUseCase;
    private final UpdateClientUseCase updateClientUseCase;
    private final DeleteClientUseCase deleteClientUseCase;

    private final ClientDtoMapper clientDtoMapper;

    @GetMapping
    @Operation(summary = "Get all clients", description = "Retrieve all registered clients")
    public ResponseEntity<List<ClientDTO>> getAllClients() {
        log.info("GET /api/clients - Retrieving all clients");

        try {
            Long companyId = CompanyContext.getCompanyId();

            // If no company context, return empty list (user needs to select/create company
            // first)
            if (companyId == null) {
                log.warn("No company context found - returning empty client list");
                return ResponseEntity.ok(java.util.Collections.emptyList());
            }

            // Execute use case
            List<Client> clients = getAllClientsUseCase.execute(companyId);

            // Map to DTOs
            List<ClientDTO> dtos = clients.stream()
                    .map(clientDtoMapper::toDto)
                    .collect(Collectors.toList());

            log.info("Found {} clients", dtos.size());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            log.error("Error retrieving clients: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get client by ID", description = "Retrieve a specific client by its ID")
    public ResponseEntity<ClientDTO> getClientById(@PathVariable Long id) {
        log.info("GET /api/clients/{} - Retrieving client", id);

        try {
            // Execute use case
            Client client = getClientByIdUseCase.execute(id);

            log.info("Client found: {}", client.getBusinessName());
            return ResponseEntity.ok(clientDtoMapper.toDto(client));
        } catch (ResourceNotFoundException e) {
            log.warn("Client not found with id: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error retrieving client {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping
    @Operation(summary = "Create client", description = "Create a new client")
    public ResponseEntity<?> createClient(@RequestBody ClientDTO clientDTO) {
        log.info("POST /api/clients - Creating client: {}", clientDTO.getBusinessName());

        try {
            Long companyId = CompanyContext.getCompanyId();

            // Execute use case
            Client client = createClientUseCase.execute(
                    companyId,
                    clientDTO.getBusinessName(),
                    clientDTO.getTaxId(),
                    clientDTO.getEmail());

            ClientDTO response = clientDtoMapper.toDto(client);
            log.info("Client created successfully with id: {}", client.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.error("Validation error creating client: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .code("VALIDATION_ERROR")
                            .message(e.getMessage())
                            .timestamp(Instant.now())
                            .build());
        } catch (Exception e) {
            log.error("Error creating client: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update client", description = "Update an existing client")
    public ResponseEntity<?> updateClient(@PathVariable Long id, @RequestBody ClientDTO clientDTO) {
        log.info("PUT /api/clients/{} - Updating client", id);

        try {
            // Execute use case
            Client client = updateClientUseCase.execute(
                    id,
                    clientDTO.getBusinessName(),
                    clientDTO.getTaxId(),
                    clientDTO.getEmail());

            ClientDTO response = clientDtoMapper.toDto(client);
            log.info("Client updated successfully: {}", id);
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            log.warn("Client not found with id: {}", id);
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            log.error("Validation error updating client: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .code("VALIDATION_ERROR")
                            .message(e.getMessage())
                            .timestamp(Instant.now())
                            .build());
        } catch (Exception e) {
            log.error("Error updating client {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete client", description = "Delete a client by ID")
    public ResponseEntity<?> deleteClient(@PathVariable Long id) {
        log.info("DELETE /api/clients/{} - Deleting client", id);

        try {
            // Execute use case
            deleteClientUseCase.execute(id);

            log.info("Client deleted successfully: {}", id);
            return ResponseEntity.noContent().build();
        } catch (ResourceNotFoundException e) {
            log.warn("Client not found with id: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error deleting client {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
