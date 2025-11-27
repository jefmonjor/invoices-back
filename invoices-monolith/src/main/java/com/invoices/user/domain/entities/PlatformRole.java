package com.invoices.user.domain.entities;

/**
 * Platform-level roles for system-wide permissions.
 * 
 * This is SEPARATE from company-level roles (ADMIN/USER in UserCompany).
 * 
 * Platform roles:
 * - PLATFORM_ADMIN: Super administrator with platform-wide privileges
 * - Can manage ALL companies
 * - Can delete/deactivate companies
 * - CANNOT access individual invoice data (privacy/security)
 * 
 * - REGULAR_USER: Normal user
 * - Can belong to multiple companies
 * - Has company-level roles (ADMIN/USER per company)
 * - Can access invoice data within their companies
 */
public enum PlatformRole {

    /**
     * Platform Administrator - System-wide management privileges
     */
    PLATFORM_ADMIN("Platform Administrator"),

    /**
     * Regular User - Standard platform access
     */
    REGULAR_USER("Regular User");

    private final String displayName;

    PlatformRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Check if this role is a platform administrator
     */
    public boolean isPlatformAdmin() {
        return this == PLATFORM_ADMIN;
    }
}
