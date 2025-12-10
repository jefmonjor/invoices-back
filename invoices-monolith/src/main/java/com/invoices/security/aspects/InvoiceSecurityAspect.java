package com.invoices.security.aspects;

import com.invoices.security.exceptions.PlatformAdminAccessDeniedException;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Security Aspect to prevent Platform Administrators from accessing invoice
 * and client data.
 * 
 * This is a CRITICAL security control that enforces separation of duties:
 * - Platform Admins can manage companies (create, delete, view stats)
 * - Platform Admins CANNOT access individual invoice/client data
 * (privacy/security)
 * 
 * Executes BEFORE any invoice-related method to block access at infrastructure
 * level.
 * 
 * Note: CompanyRepository is EXCLUDED because Platform Admin needs access to
 * company statistics (count, list, etc.)
 */
@Aspect
@Component
@Order(1) // Execute FIRST before any other aspect or business logic
public class InvoiceSecurityAspect {

    /**
     * Pointcut for invoice-specific operations.
     * Matches InvoiceRepository, InvoiceService, InvoiceController, etc.
     */
    @Pointcut("execution(* com.invoices.invoice..*Invoice*.*(..))")
    public void invoiceOperations() {
    }

    /**
     * Pointcut for client-specific operations.
     * Matches ClientRepository, ClientService, ClientController, etc.
     */
    @Pointcut("execution(* com.invoices.invoice..*Client*.*(..))")
    public void clientOperations() {
    }

    /**
     * Pointcut for VeriFactu metrics (invoice-related).
     */
    @Pointcut("execution(* com.invoices.invoice..*VerifactuMetrics*.*(..))")
    public void verifactuMetricsOperations() {
    }

    /**
     * Combined pointcut for all invoice-related data that Platform Admin cannot
     * access.
     * Note: CompanyRepository is NOT included here - Platform Admin can access
     * company data.
     */
    @Pointcut("invoiceOperations() || clientOperations() || verifactuMetricsOperations()")
    public void restrictedInvoiceOperations() {
    }

    /**
     * Intercept invoice and client method calls.
     * Throws exception if caller has PLATFORM_ADMIN role.
     */
    @Before("restrictedInvoiceOperations()")
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
