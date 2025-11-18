package com.invoices.invoice.domain.ports;

import com.invoices.invoice.domain.entities.Client;

import java.util.List;
import java.util.Optional;

/**
 * Port for Client repository.
 * Domain interface - implementations are in infrastructure layer.
 */
public interface ClientRepository {

    Client save(Client client);

    Optional<Client> findById(Long id);

    Optional<Client> findByTaxId(String taxId);

    List<Client> findAll();

    void deleteById(Long id);

    boolean existsById(Long id);
}
