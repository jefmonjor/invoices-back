package com.invoices.security;

import com.invoices.security.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("companySecurity")
@RequiredArgsConstructor
@Slf4j
public class CompanySecurity {

    private final PermissionService permissionService;

    /**
     * Check if the current user has access to the company with the specified role.
     * Used in SpEL expressions: @companySecurity.hasCompanyAccess(#companyId,
     * 'ADMIN')
     *
     * @param companyId    The company ID
     * @param requiredRole The required role (e.g., "ADMIN", "USER")
     * @return true if access is granted
     */
    public boolean hasCompanyAccess(Long companyId, String requiredRole) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        // Platform ADMIN has access to everything
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return true;
        }

        Long userId;
        try {
            userId = Long.parseLong(auth.getName());
        } catch (NumberFormatException e) {
            log.error("Invalid user ID in authentication: {}", auth.getName());
            return false;
        }

        String userRole = permissionService.getUserRoleInCompany(userId, companyId);
        if (userRole == null) {
            return false;
        }

        if ("ADMIN".equals(requiredRole)) {
            return "ADMIN".equals(userRole);
        }

        // If required role is USER, both ADMIN and USER can access
        return true;
    }
}
