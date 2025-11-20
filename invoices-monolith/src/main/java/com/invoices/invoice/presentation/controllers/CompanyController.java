package com.invoices.invoice.presentation.controllers;

import com.invoices.invoice.domain.entities.Company;
import com.invoices.invoice.domain.ports.CompanyRepository;
import com.invoices.invoice.dto.CompanyDTO;
import com.invoices.invoice.presentation.mappers.CompanyDtoMapper;
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
 * REST controller for Company operations.
 */
@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Companies", description = "Endpoints for company management")
public class CompanyController {

    private final CompanyRepository companyRepository;
    private final CompanyDtoMapper companyDtoMapper;

    @GetMapping
    @Operation(summary = "Get all companies", description = "Retrieve all registered companies")
    public ResponseEntity<List<CompanyDTO>> getAllCompanies() {
        log.info("GET /api/companies - Retrieving all companies");

        List<CompanyDTO> companies = companyRepository.findAll().stream()
                .map(companyDtoMapper::toDto)
                .collect(Collectors.toList());

        log.info("Found {} companies", companies.size());
        return ResponseEntity.ok(companies);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get company by ID", description = "Retrieve a specific company by its ID")
    public ResponseEntity<CompanyDTO> getCompanyById(@PathVariable Long id) {
        log.info("GET /api/companies/{} - Retrieving company", id);

        return companyRepository.findById(id)
                .map(company -> {
                    log.info("Company found: {}", company.getBusinessName());
                    return ResponseEntity.ok(companyDtoMapper.toDto(company));
                })
                .orElseGet(() -> {
                    log.warn("Company not found with id: {}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @PostMapping
    @Operation(summary = "Create company", description = "Create a new company")
    public ResponseEntity<CompanyDTO> createCompany(@RequestBody CompanyDTO companyDTO) {
        log.info("POST /api/companies - Creating company: {}", companyDTO.getBusinessName());

        try {
            Company company = companyDtoMapper.toDomain(companyDTO);
            Company savedCompany = companyRepository.save(company);
            CompanyDTO response = companyDtoMapper.toDto(savedCompany);

            log.info("Company created successfully with id: {}", savedCompany.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error creating company: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update company", description = "Update an existing company")
    public ResponseEntity<CompanyDTO> updateCompany(
            @PathVariable Long id,
            @RequestBody CompanyDTO companyDTO) {
        log.info("PUT /api/companies/{} - Updating company", id);

        if (!companyRepository.existsById(id)) {
            log.warn("Company not found with id: {}", id);
            return ResponseEntity.notFound().build();
        }

        try {
            companyDTO.setId(id);
            Company company = companyDtoMapper.toDomain(companyDTO);
            Company savedCompany = companyRepository.save(company);
            CompanyDTO response = companyDtoMapper.toDto(savedCompany);

            log.info("Company updated successfully: {}", savedCompany.getBusinessName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating company: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete company", description = "Delete a company by its ID")
    public ResponseEntity<Void> deleteCompany(@PathVariable Long id) {
        log.info("DELETE /api/companies/{} - Deleting company", id);

        if (!companyRepository.existsById(id)) {
            log.warn("Company not found with id: {}", id);
            return ResponseEntity.notFound().build();
        }

        companyRepository.deleteById(id);
        log.info("Company deleted successfully with id: {}", id);
        return ResponseEntity.noContent().build();
    }
}
