package com.invoices.security.service;

import com.invoices.company.infrastructure.persistence.entities.UserCompany;
import com.invoices.company.infrastructure.persistence.entities.UserCompanyId;
import com.invoices.company.infrastructure.persistence.repositories.UserCompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service for checking user permissions based on their role in companies.
 * Implements role-based access control (RBAC) for multi-company environment.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionService {

    private final UserCompanyRepository userCompanyRepository;

    /**
     * Check if user can edit an invoice.
     * Only ADMIN users can edit invoices.
     */
    public boolean canEditInvoice(Authentication auth, Long invoiceId) {
        if (auth == null || !auth.isAuthenticated()) {
            log.warn("Unauthenticated user attempted to edit invoice {}", invoiceId);
            return false;
        }

        return hasRole(auth, "ROLE_ADMIN");
    }

    /**
     * Check if user can delete an invoice.
     * Only ADMIN users can delete invoices.
     */
    public boolean canDeleteInvoice(Authentication auth, Long invoiceId) {
        if (auth == null || !auth.isAuthenticated()) {
            log.warn("Unauthenticated user attempted to delete invoice {}", invoiceId);
            return false;
        }

        return hasRole(auth, "ROLE_ADMIN");
    }

    /**
     * Check if user can manage users in a company.
     * Only ADMIN users of that specific company can manage users.
     */
    public boolean canManageUsers(Authentication auth, Long companyId) {
        if (auth == null || !auth.isAuthenticated()) {
            log.warn("Unauthenticated user attempted to manage users in company {}", companyId);
            return false;
        }

        Long userId = getUserIdFromAuth(auth);
        if (userId == null) {
            log.warn("Could not determine user ID from authentication for managing users in company {}", companyId);
            return false;
        }

        // Must be ADMIN in the specific company to manage users
        // Global ADMIN role alone is not sufficient - must be member of company
        return isAdminInCompany(userId, companyId);
    }

    /**
     * Check if user can edit a company.
     * Only ADMIN users can edit companies.
     */
    public boolean canEditCompany(Authentication auth, Long companyId) {
        if (auth == null || !auth.isAuthenticated()) {
            log.warn("Unauthenticated user attempted to edit company {}", companyId);
            return false;
        }

        // Global ADMIN can edit any company
        if (hasRole(auth, "ROLE_ADMIN")) {
            return true;
        }

        // Company ADMIN can edit their own company
        Long userId = getUserIdFromAuth(auth);
        if (userId == null) {
            log.warn("Could not determine user ID from authentication for editing company {}", companyId);
            return false;
        }
        return isAdminInCompany(userId, companyId);
    }

    /**
     * Check if user can create a new company.
     * Only ADMIN users can create companies.
     */
    public boolean canCreateCompany(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            log.warn("Unauthenticated user attempted to create company");
            return false;
        }

        return hasRole(auth, "ROLE_ADMIN");
    }

    /**
     * Check if a user is ADMIN in a specific company.
     * 
     * @param userId    User ID
     * @param companyId Company ID
     * @return true if user is ADMIN in the company
     */
    public boolean isAdminInCompany(Long userId, Long companyId) {
        UserCompanyId id = new UserCompanyId(userId, companyId);
        Optional<UserCompany> userCompany = userCompanyRepository.findById(id);

        if (userCompany.isEmpty()) {
            log.warn("User {} does not belong to company {}", userId, companyId);
            return false;
        }

        return "ADMIN".equals(userCompany.get().getRole());
    }

    /**
     * Get user's role in a specific company.
     * 
     * @param userId    User ID
     * @param companyId Company ID
     * @return Role string ("ADMIN" or "USER"), or null if user doesn't belong to
     *         company
     */
    public String getUserRoleInCompany(Long userId, Long companyId) {
        UserCompanyId id = new UserCompanyId(userId, companyId);
        Optional<UserCompany> userCompany = userCompanyRepository.findById(id);

        return userCompany.map(UserCompany::getRole).orElse(null);
    }

    /**
     * Check if authentication has a specific role.
     */
    private boolean hasRole(Authentication auth, String role) {
        return auth.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(role));
    }

    /**
     * Check if user belongs to a company.
     */
    public boolean belongsToCompany(Long userId, Long companyId) {
        UserCompanyId id = new UserCompanyId(userId, companyId);
        return userCompanyRepository.findById(id).isPresent();
    }

    /**
     * Extract user ID from authentication object.
     * Assumes authentication name is the user ID.
     */
    private Long getUserIdFromAuth(Authentication auth) {
        try {
            return Long.parseLong(auth.getName());
        } catch (NumberFormatException e) {
            log.error("Unable to parse user ID from authentication name: {}", auth.getName());
            return null;
        }
    }
}
