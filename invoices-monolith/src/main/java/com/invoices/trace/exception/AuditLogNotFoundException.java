package com.invoices.trace_service.exception;

public class AuditLogNotFoundException extends RuntimeException {
    public AuditLogNotFoundException(String message) {
        super(message);
    }

    public AuditLogNotFoundException(Long auditLogId) {
        super("Audit log not found with ID: " + auditLogId);
    }
}
