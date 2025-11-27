package com.invoices.security.exceptions;

/**
 * Exception thrown when a Platform Administrator attempts to access invoice
 * data.
 * 
 * Platform Administrators have elevated privileges to manage companies,
 * but are explicitly prohibited from accessing invoice data for
 * privacy/security reasons.
 */
public class PlatformAdminAccessDeniedException extends RuntimeException {

    public PlatformAdminAccessDeniedException(String message) {
        super(message);
    }

    public PlatformAdminAccessDeniedException(String message, Throwable cause) {
        super(message, cause);
    }
}
