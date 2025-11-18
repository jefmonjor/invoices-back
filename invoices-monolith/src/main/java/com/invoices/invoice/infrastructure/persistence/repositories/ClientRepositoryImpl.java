package com.invoices.invoice.infrastructure.persistence.repositories;

import com.invoices.invoice.domain.entities.Client;
import com.invoices.invoice.domain.ports.ClientRepository;
import com.invoices.invoice.infrastructure.persistence.mappers.ClientJpaMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of ClientRepository port.
 * Adapter between domain and JPA persistence.
 */
@Repository
public class ClientRepositoryImpl implements ClientRepository {

    private final JpaClientRepository jpaRepository;
    private final ClientJpaMapper mapper;

    public ClientRepositoryImpl(JpaClientRepository jpaRepository, ClientJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Client save(Client client) {
        var jpaEntity = mapper.toJpaEntity(client);
        var savedEntity = jpaRepository.save(jpaEntity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Client> findById(Long id) {
        return jpaRepository.findById(id)
            .map(mapper::toDomain);
    }

    @Override
    public Optional<Client> findByTaxId(String taxId) {
        return jpaRepository.findByTaxId(taxId)
            .map(mapper::toDomain);
    }

    @Override
    public List<Client> findAll() {
        return jpaRepository.findAll().stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return jpaRepository.existsById(id);
    }
}
