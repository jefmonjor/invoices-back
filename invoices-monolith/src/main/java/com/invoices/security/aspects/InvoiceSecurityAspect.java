package com.invoices.security.aspects;

import com.invoices.security.exceptions.PlatformAdminAccessDeniedException;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Security Aspect to prevent Platform Administrators from accessing invoice
 * data.
 * 
 * This is a CRITICAL security control that enforces separation of duties:
 * - Platform Admins can manage companies (create, delete, view stats)
 * - Platform Admins CANNOT access individual invoice data (privacy/security)
 * 
 * Executes BEFORE any invoice-related method to block access at infrastructure
 * level.
 * 
 * Coverage: All methods in com.invoices.invoice package (controllers, services,
 * repositories)
 */
@Aspect
@Component
@Order(1) // Execute FIRST before any other aspect or business logic
public class InvoiceSecurityAspect {

    /**
     * Intercept all method calls within the invoice package.
     * Throws exception if caller has PLATFORM_ADMIN role.
     */
    @Before("execution(* com.invoices.invoice..*(..))")
    public void blockPlatformAdminAccess(JoinPoint joinPoint) {
        if (isPlatformAdmin()) {
            String methodName = joinPoint.getSignature().toShortString();
            throw new PlatformAdminAccessDeniedException(
                    String.format(
                            "Access denied: Platform administrators cannot access invoice data. " +
                                    "Attempted to call: %s",
                            methodName));
        }
    }

    /**
     * Check if current user has PLATFORM_ADMIN role.
     */
    private boolean isPlatformAdmin() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        // Check if user has PLATFORM_ADMIN authority
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> "ROLE_PLATFORM_ADMIN".equals(authority) ||
                        "PLATFORM_ADMIN".equals(authority));
    }
}
