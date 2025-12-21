package com.invoices.company.application.services;

import com.invoices.company.domain.entities.UserCompany;
import com.invoices.company.domain.entities.UserCompanyId;
import com.invoices.company.domain.ports.UserCompanyRepository;
import com.invoices.invoice.domain.entities.Company;
import com.invoices.invoice.domain.entities.InvoiceStatus;
import com.invoices.invoice.domain.ports.CompanyRepository;
import com.invoices.user.domain.entities.User;
import com.invoices.user.domain.ports.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CompanyManagementService {

    private static final Logger log = LoggerFactory.getLogger(CompanyManagementService.class);

    private final CompanyRepository companyRepository;
    private final UserCompanyRepository userCompanyRepository;
    private final UserRepository userRepository;
    private final com.invoices.invoice.domain.ports.InvoiceRepository invoiceRepository;
    private final com.invoices.document.domain.ports.FileStorageService fileStorageService;
    private final com.invoices.document.domain.services.StorageUrlResolver storageUrlResolver;

    public CompanyManagementService(CompanyRepository companyRepository,
            UserCompanyRepository userCompanyRepository,
            UserRepository userRepository,
            com.invoices.invoice.domain.ports.InvoiceRepository invoiceRepository,
            com.invoices.document.domain.ports.FileStorageService fileStorageService,
            com.invoices.document.domain.services.StorageUrlResolver storageUrlResolver) {
        this.companyRepository = companyRepository;
        this.userCompanyRepository = userCompanyRepository;
        this.userRepository = userRepository;
        this.invoiceRepository = invoiceRepository;
        this.fileStorageService = fileStorageService;
        this.storageUrlResolver = storageUrlResolver;
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

        // Use JOIN FETCH to avoid N+1 queries (single query instead of 1 + N)
        List<UserCompany> userCompanies = userCompanyRepository.findByIdUserIdWithCompanyFetch(user.getId());

        return userCompanies.stream()
                .map(uc -> {
                    Company company = uc.getCompany();
                    if (company == null) {
                        log.warn(
                                "Data inconsistency: UserCompany record exists for companyId {} but Company entity is missing",
                                uc.getId().getCompanyId());
                        return null;
                    }
                    boolean isDefault = company.getId().equals(user.getCurrentCompanyId());
                    // Use URL resolver to convert logoUrl objectName to full S3 URL
                    return com.invoices.company.presentation.dto.CompanyDto.fromEntity(company, uc.getRole(),
                            isDefault, storageUrlResolver);
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

        // Get all users for the company with JOIN FETCH to avoid N+1 queries
        List<UserCompany> userCompanies = userCompanyRepository.findByIdCompanyIdWithUserFetch(companyId);

        return userCompanies.stream()
                .map(uc -> {
                    User user = uc.getUser();
                    if (user == null) {
                        throw new RuntimeException("User not found");
                    }

                    String fullName = user.getFirstName() + " " + user.getLastName();

                    return com.invoices.company.presentation.dto.UserCompanyDto.builder()
                            .userId(user.getId())
                            .name(fullName)
                            .email(user.getEmail())
                            .role(uc.getRole())
                            .joinedAt(null)
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
            // Use optimized query to count admins instead of loading all users
            long adminCount = userCompanyRepository.countByIdCompanyIdAndRole(companyId, "ADMIN");

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
            // Use optimized query to count admins instead of loading all users
            long adminCount = userCompanyRepository.countByIdCompanyIdAndRole(companyId, "ADMIN");

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
     * Optimized: Uses count queries instead of loading all invoices into memory.
     */
    public com.invoices.company.presentation.dto.CompanyMetricsDto getCompanyMetrics(Long companyId) {
        // Optimized: Use repository methods for counting and summing to avoid loading
        // all invoices
        long totalInvoices = invoiceRepository.countByCompanyId(companyId);
        long paidInvoices = invoiceRepository.countByCompanyIdAndStatus(companyId, InvoiceStatus.PAID.name());
        long pendingInvoices = totalInvoices - paidInvoices;

        java.math.BigDecimal totalRevenue = invoiceRepository.sumTotalAmountByCompanyId(companyId);

        // Sum revenue for all statuses except PAID
        java.math.BigDecimal pendingRevenue = invoiceRepository.sumTotalAmountByCompanyIdAndStatus(companyId,
                InvoiceStatus.PENDING.name());
        // Note: For a more comprehensive "pending" revenue, we might want to include
        // other statuses
        // like DRAFT or FINALIZED (if not PAID), but following the original logic of
        // total - paid.

        // Count unique clients
        // NOTE: This could also be optimized with a repository method
        // countUniqueClientsByCompanyId
        List<com.invoices.invoice.domain.entities.Invoice> invoices = invoiceRepository.findByCompanyId(companyId);
        long totalClients = invoices.stream()
                .map(com.invoices.invoice.domain.entities.Invoice::getClientId)
                .distinct()
                .count();

        // Count active users in this company using optimized count query
        long activeUsers = userCompanyRepository.countByIdCompanyId(companyId);

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

    /**
     * Upload company logo to S3 and update company with logoUrl.
     */
    @Transactional
    public Company uploadLogo(Long companyId, org.springframework.web.multipart.MultipartFile file) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        try {
            // Generate unique filename
            String objectName = "logos/company-" + companyId + "-" + System.currentTimeMillis() + ".png";
            byte[] fileBytes = file.getBytes();

            // Create FileContent
            com.invoices.document.domain.entities.FileContent fileContent = new com.invoices.document.domain.entities.FileContent(
                    () -> new java.io.ByteArrayInputStream(fileBytes),
                    fileBytes.length,
                    "image/png");

            // Upload to S3
            fileStorageService.storeFile(objectName, fileContent);

            // Update company with logo object name (will be resolved to URL when needed)
            Company updatedCompany = company.withLogoUrl(objectName);
            return companyRepository.save(updatedCompany);
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to upload logo: " + e.getMessage(), e);
        }
    }

    /**
     * Delete company logo from S3 and remove logoUrl from company.
     */
    @Transactional
    public Company deleteLogo(Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        String logoUrl = company.getLogoUrl();
        if (logoUrl != null && !logoUrl.isEmpty()) {
            // Delete from S3 (best effort)
            try {
                fileStorageService.deleteFile(logoUrl);
            } catch (Exception e) {
                log.warn("Failed to delete logo file: {}", e.getMessage());
            }
        }

        // Update company to remove logo URL
        Company updatedCompany = company.withLogoUrl(null);
        return companyRepository.save(updatedCompany);
    }
}
