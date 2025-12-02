package com.invoices.invoice.domain.usecases;

import com.invoices.invoice.domain.entities.Client;
import com.invoices.invoice.domain.ports.ClientRepository;
import com.invoices.invoice.domain.ports.CompanyRepository;
import com.invoices.shared.domain.exception.ResourceNotFoundException;

import java.util.List;

/**
 * UseCase for retrieving all clients of a company.
 *
 * Encapsulates the business logic for listing clients with company validation.
 */
public class GetAllClientsUseCase {

    private final ClientRepository clientRepository;
    private final CompanyRepository companyRepository;

    public GetAllClientsUseCase(
            ClientRepository clientRepository,
            CompanyRepository companyRepository) {
        this.clientRepository = clientRepository;
        this.companyRepository = companyRepository;
    }

    /**
     * Retrieves all clients for a specific company.
     *
     * Business logic:
     * 1. Validate company exists
     * 2. Retrieve all clients for the company
     *
     * @param companyId ID of the company
     * @return List of clients (empty list if no clients exist)
     * @throws ResourceNotFoundException if company doesn't exist
     * @throws IllegalArgumentException if companyId is null
     */
    public List<Client> execute(Long companyId) {
        if (companyId == null) {
            throw new IllegalArgumentException("Company ID cannot be null");
        }

        // Validate company exists
        if (!companyRepository.existsById(companyId)) {
            throw new ResourceNotFoundException("Company not found with id: " + companyId);
        }

        // Return all clients for the company
        return clientRepository.findByCompanyId(companyId);
    }
}
