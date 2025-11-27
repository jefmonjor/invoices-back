package com.invoices.security.utils;

import com.invoices.user.domain.entities.PlatformRole;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utility class for security operations.
 */
public class SecurityUtils {

    /**
     * Check if the current user is a Platform Administrator.
     *
     * @return true if platform admin, false otherwise
     */
    public static boolean isPlatformAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_" + PlatformRole.PLATFORM_ADMIN.name()) ||
                        role.equals(PlatformRole.PLATFORM_ADMIN.name()));
    }

    /**
     * Require the current user to be a Platform Administrator.
     * Throws AccessDeniedException if not.
     */
    public static void requirePlatformAdmin() {
        if (!isPlatformAdmin()) {
            throw new AccessDeniedException("Platform Administrator privileges required");
        }
    }
}
