package com.invoices.invoice.presentation.controllers;

import com.invoices.invoice.domain.entities.Client;
import com.invoices.invoice.domain.ports.ClientRepository;
import com.invoices.invoice.dto.ClientDTO;
import com.invoices.invoice.presentation.mappers.ClientDtoMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for Client operations.
 */
@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Clients", description = "Endpoints for client management")
public class ClientController {

    private final ClientRepository clientRepository;
    private final ClientDtoMapper clientDtoMapper;

    @GetMapping
    @Operation(summary = "Get all clients", description = "Retrieve all registered clients")
    public ResponseEntity<List<ClientDTO>> getAllClients() {
        log.info("GET /api/clients - Retrieving all clients");

        List<ClientDTO> clients = clientRepository.findAll().stream()
                .map(clientDtoMapper::toDto)
                .collect(Collectors.toList());

        log.info("Found {} clients", clients.size());
        return ResponseEntity.ok(clients);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get client by ID", description = "Retrieve a specific client by its ID")
    public ResponseEntity<ClientDTO> getClientById(@PathVariable Long id) {
        log.info("GET /api/clients/{} - Retrieving client", id);

        return clientRepository.findById(id)
                .map(client -> {
                    log.info("Client found: {}", client.getBusinessName());
                    return ResponseEntity.ok(clientDtoMapper.toDto(client));
                })
                .orElseGet(() -> {
                    log.warn("Client not found with id: {}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @PostMapping
    @Operation(summary = "Create client", description = "Create a new client")
    public ResponseEntity<ClientDTO> createClient(@RequestBody ClientDTO clientDTO) {
        log.info("POST /api/clients - Creating client: {}", clientDTO.getBusinessName());

        try {
            Client client = clientDtoMapper.toDomain(clientDTO);
            Client savedClient = clientRepository.save(client);
            ClientDTO response = clientDtoMapper.toDto(savedClient);

            log.info("Client created successfully with id: {}", savedClient.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error creating client: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update client", description = "Update an existing client")
    public ResponseEntity<ClientDTO> updateClient(
            @PathVariable Long id,
            @RequestBody ClientDTO clientDTO) {
        log.info("PUT /api/clients/{} - Updating client", id);

        if (!clientRepository.existsById(id)) {
            log.warn("Client not found with id: {}", id);
            return ResponseEntity.notFound().build();
        }

        try {
            clientDTO.setId(id);
            Client client = clientDtoMapper.toDomain(clientDTO);
            Client savedClient = clientRepository.save(client);
            ClientDTO response = clientDtoMapper.toDto(savedClient);

            log.info("Client updated successfully: {}", savedClient.getBusinessName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating client: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete client", description = "Delete a client by its ID")
    public ResponseEntity<Void> deleteClient(@PathVariable Long id) {
        log.info("DELETE /api/clients/{} - Deleting client", id);

        if (!clientRepository.existsById(id)) {
            log.warn("Client not found with id: {}", id);
            return ResponseEntity.notFound().build();
        }

        clientRepository.deleteById(id);
        log.info("Client deleted successfully with id: {}", id);
        return ResponseEntity.noContent().build();
    }
}
