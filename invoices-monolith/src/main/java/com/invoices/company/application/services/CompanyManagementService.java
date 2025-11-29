package com.invoices.company.application.services;

import com.invoices.company.infrastructure.persistence.entities.UserCompany;
import com.invoices.company.infrastructure.persistence.entities.UserCompanyId;
import com.invoices.company.infrastructure.persistence.repositories.UserCompanyRepository;
import com.invoices.invoice.domain.entities.Company;
import com.invoices.invoice.domain.entities.InvoiceStatus;
import com.invoices.invoice.domain.ports.CompanyRepository;
import com.invoices.user.domain.entities.User;
import com.invoices.user.domain.ports.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CompanyManagementService {

    private final CompanyRepository companyRepository;
    private final UserCompanyRepository userCompanyRepository;
    private final UserRepository userRepository;
    private final com.invoices.invoice.domain.ports.InvoiceRepository invoiceRepository;

    public CompanyManagementService(CompanyRepository companyRepository,
            UserCompanyRepository userCompanyRepository,
            UserRepository userRepository,
            com.invoices.invoice.domain.ports.InvoiceRepository invoiceRepository) {
        this.companyRepository = companyRepository;
        this.userCompanyRepository = userCompanyRepository;
        this.userRepository = userRepository;
        this.invoiceRepository = invoiceRepository;
    }

    @Transactional
    public Company createCompany(Company company, Long userId) {
        // Save company
        Company savedCompany = companyRepository.save(company);

        // Link user to company as ADMIN
        UserCompanyId id = new UserCompanyId(userId, savedCompany.getId());
        UserCompany userCompany = new UserCompany(id, "ADMIN");
        userCompanyRepository.save(userCompany);

        // Update user's current company if not set
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getCurrentCompanyId() == null) {
                userRepository.save(user.withCurrentCompany(savedCompany.getId()));
            }
        }

        return savedCompany;
    }

    @Transactional
    public void addUserToCompany(Long userId, Long companyId, String role) {
        UserCompanyId id = new UserCompanyId(userId, companyId);
        UserCompany userCompany = new UserCompany(id, role);
        userCompanyRepository.save(userCompany);
    }

    public List<com.invoices.company.presentation.dto.CompanyDto> getUserCompanies(String username) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        List<UserCompany> userCompanies = userCompanyRepository.findByIdUserId(user.getId());

        return userCompanies.stream()
                .map(uc -> {
                    Optional<Company> companyOpt = companyRepository.findById(uc.getId().getCompanyId());
                    if (companyOpt.isEmpty()) {
                        // Log warning but don't crash the entire request
                        // This indicates data inconsistency (orphaned user_company record)
                        System.err.println("WARNING: Data inconsistency found. UserCompany record exists for companyId "
                                + uc.getId().getCompanyId() + " but Company entity is missing.");
                        return null;
                    }
                    Company company = companyOpt.get();
                    boolean isDefault = company.getId().equals(user.getCurrentCompanyId());
                    return com.invoices.company.presentation.dto.CompanyDto.fromEntity(company, uc.getRole(),
                            isDefault);
                })
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional
    public void switchCompany(String username, Long companyId) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify user belongs to company
        userCompanyRepository.findById(new UserCompanyId(user.getId(), companyId))
                .orElseThrow(() -> new RuntimeException("User does not belong to this company"));

        user = user.withCurrentCompany(companyId);
        userRepository.save(user);
    }

    @Transactional
    public Company updateCompany(Long companyId, com.invoices.company.presentation.dto.UpdateCompanyRequest request) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        company = company.withDetails(
                request.getBusinessName(),
                request.getAddress(),
                request.getCity(),
                request.getPostalCode(),
                request.getProvince(),
                request.getPhone(),
                request.getEmail());

        return companyRepository.save(company);
    }

    public List<UserCompany> getUserCompanies(Long userId) {
        return userCompanyRepository.findByIdUserId(userId);
    }

    /**
     * Get user by email.
     */
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Create an additional company. Only ADMIN users can create companies.
     * The creator is automatically assigned as ADMIN of the new company.
     */
    @Transactional
    public Company createAdditionalCompany(Company company, Long userId) {
        // Verify the user is ADMIN in at least one company
        List<UserCompany> userCompanies = userCompanyRepository.findByIdUserId(userId);
        boolean isAdminAnywhere = userCompanies.stream()
                .anyMatch(uc -> "ADMIN".equals(uc.getRole()));

        if (!isAdminAnywhere && !userCompanies.isEmpty()) {
            throw new SecurityException("Only ADMIN users can create new companies");
        }

        // Verify CIF uniqueness
        if (companyRepository.findByTaxId(company.getTaxId()).isPresent()) {
            throw new IllegalArgumentException("A company with this CIF/NIF already exists");
        }

        // Create company and link user as ADMIN
        return createCompany(company, userId);
    }

    /**
     * Get all users belonging to a company with their roles.
     * Only ADMIN users can view company users.
     */
    public List<com.invoices.company.presentation.dto.UserCompanyDto> getCompanyUsers(Long companyId,
            Long requestingUserId) {
        // Verify requesting user is ADMIN of the company
        UserCompanyId requesterId = new UserCompanyId(requestingUserId, companyId);
        UserCompany requesterUserCompany = userCompanyRepository.findById(requesterId)
                .orElseThrow(() -> new SecurityException("You do not have access to this company"));

        if (!"ADMIN".equals(requesterUserCompany.getRole())) {
            throw new SecurityException("Only ADMIN users can view company users");
        }

        // Get all users for the company
        List<UserCompany> userCompanies = userCompanyRepository.findByIdCompanyId(companyId);

        return userCompanies.stream()
                .map(uc -> {
                    User user = userRepository.findById(uc.getId().getUserId())
                            .orElseThrow(() -> new RuntimeException("User not found"));

                    String fullName = user.getFirstName() + " " + user.getLastName();

                    return com.invoices.company.presentation.dto.UserCompanyDto.builder()
                            .userId(user.getId())
                            .name(fullName)
                            .email(user.getEmail())
                            .role(uc.getRole())
                            .joinedAt(null) // TODO: Add join timestamp to UserCompany entity
                            .build();
                })
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Remove a user from a company.
     * Validation: Cannot remove the last ADMIN user from a company.
     */
    @Transactional
    public void removeUserFromCompany(Long companyId, Long userId, Long requestingUserId) {
        // Verify requesting user is ADMIN
        UserCompanyId requesterId = new UserCompanyId(requestingUserId, companyId);
        UserCompany requesterUserCompany = userCompanyRepository.findById(requesterId)
                .orElseThrow(() -> new SecurityException("You do not have access to this company"));

        if (!"ADMIN".equals(requesterUserCompany.getRole())) {
            throw new SecurityException("Only ADMIN users can remove users from company");
        }

        // Get user to remove
        UserCompanyId userToRemoveId = new UserCompanyId(userId, companyId);
        UserCompany userToRemove = userCompanyRepository.findById(userToRemoveId)
                .orElseThrow(() -> new RuntimeException("User is not part of this company"));

        // Check if removing user would leave company without ADMIN
        if ("ADMIN".equals(userToRemove.getRole())) {
            long adminCount = userCompanyRepository.findByIdCompanyId(companyId).stream()
                    .filter(uc -> "ADMIN".equals(uc.getRole()))
                    .count();

            if (adminCount <= 1) {
                throw new IllegalStateException("Cannot remove the last ADMIN user from the company");
            }
        }

        // Remove user from company
        userCompanyRepository.delete(userToRemove);

        // If removed user had this as current company, update to null or first
        // available company
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (companyId.equals(user.getCurrentCompanyId())) {
                // Find first available company for this user
                List<UserCompany> remainingCompanies = userCompanyRepository.findByIdUserId(userId);
                if (!remainingCompanies.isEmpty()) {
                    userRepository.save(user.withCurrentCompany(remainingCompanies.get(0).getId().getCompanyId()));
                } else {
                    userRepository.save(user.withCurrentCompany(null));
                }
            }
        }
    }

    /**
     * Update a user's role in a company.
     * Validation: Cannot change role of last ADMIN to USER.
     */
    @Transactional
    public void updateUserRole(Long companyId, Long userId, String newRole, Long requestingUserId) {
        // Verify requesting user is ADMIN
        UserCompanyId requesterId = new UserCompanyId(requestingUserId, companyId);
        UserCompany requesterUserCompany = userCompanyRepository.findById(requesterId)
                .orElseThrow(() -> new SecurityException("You do not have access to this company"));

        if (!"ADMIN".equals(requesterUserCompany.getRole())) {
            throw new SecurityException("Only ADMIN users can change user roles");
        }

        // Get user to update
        UserCompanyId userToUpdateId = new UserCompanyId(userId, companyId);
        UserCompany userToUpdate = userCompanyRepository.findById(userToUpdateId)
                .orElseThrow(() -> new RuntimeException("User is not part of this company"));

        // If changing from ADMIN to USER, check if it's the last ADMIN
        if ("ADMIN".equals(userToUpdate.getRole()) && "USER".equals(newRole)) {
            long adminCount = userCompanyRepository.findByIdCompanyId(companyId).stream()
                    .filter(uc -> "ADMIN".equals(uc.getRole()))
                    .count();

            if (adminCount <= 1) {
                throw new IllegalStateException("Cannot change role of the last ADMIN user");
            }
        }

        // Update role
        userToUpdate.setRole(newRole);
        userCompanyRepository.save(userToUpdate);
    }

    /**
     * Get company metrics including invoices, revenue, clients, and users.
     */
    public com.invoices.company.presentation.dto.CompanyMetricsDto getCompanyMetrics(Long companyId) {
        // Get all invoices for the company
        List<com.invoices.invoice.domain.entities.Invoice> invoices = invoiceRepository.findByCompanyId(companyId);

        long totalInvoices = invoices.size();
        long paidInvoices = invoices.stream()
                .filter(inv -> InvoiceStatus.PAID.equals(inv.getStatus()))
                .count();
        long pendingInvoices = totalInvoices - paidInvoices;

        java.math.BigDecimal totalRevenue = invoices.stream()
                .map(com.invoices.invoice.domain.entities.Invoice::getTotalAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        java.math.BigDecimal pendingRevenue = invoices.stream()
                .filter(inv -> !InvoiceStatus.PAID.equals(inv.getStatus()))
                .map(com.invoices.invoice.domain.entities.Invoice::getTotalAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        // Count unique clients (assuming we have access to client IDs from invoices)
        long totalClients = invoices.stream()
                .map(com.invoices.invoice.domain.entities.Invoice::getClientId)
                .distinct()
                .count();

        // Count active users in this company
        long activeUsers = userCompanyRepository.findByIdCompanyId(companyId).size();

        return com.invoices.company.presentation.dto.CompanyMetricsDto.builder()
                .totalInvoices(totalInvoices)
                .paidInvoices(paidInvoices)
                .pendingInvoices(pendingInvoices)
                .totalRevenue(totalRevenue)
                .pendingRevenue(pendingRevenue)
                .totalClients(totalClients)
                .activeUsers(activeUsers)
                .periodStart(java.time.LocalDate.now().minusMonths(12))
                .periodEnd(java.time.LocalDate.now())
                .build();
    }

    /**
     * Delete a company.
     * Validation: Cannot delete company with existing invoices.
     */
    @Transactional
    public void deleteCompany(Long companyId) {
        long invoiceCount = invoiceRepository.countByCompanyId(companyId);
        if (invoiceCount > 0) {
            throw new IllegalStateException(
                    "Cannot delete company with " + invoiceCount + " existing invoices");
        }

        // Delete all user-company relationships
        List<UserCompany> userCompanies = userCompanyRepository.findByIdCompanyId(companyId);
        userCompanyRepository.deleteAll(userCompanies);

        // Delete company
        companyRepository.deleteById(companyId);
    }
}
