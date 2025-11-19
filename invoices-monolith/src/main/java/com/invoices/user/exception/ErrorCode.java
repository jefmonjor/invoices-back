package com.invoices.user.exception;

/**
 * Enum defining error codes for the application.
 */
public enum ErrorCode {

    // User errors (1xxx)
    USER_NOT_FOUND("1001", "User not found"),
    USER_ALREADY_EXISTS("1002", "User already exists"),
    USER_DISABLED("1003", "User account is disabled"),

    // Authentication errors (2xxx)
    INVALID_CREDENTIALS("2001", "Invalid credentials"),
    INVALID_TOKEN("2002", "Invalid authentication token"),
    TOKEN_EXPIRED("2003", "Authentication token has expired"),
    UNAUTHORIZED("2004", "Unauthorized access"),

    // Validation errors (3xxx)
    VALIDATION_FAILED("3001", "Validation failed"),
    INVALID_INPUT("3002", "Invalid input data"),

    // General errors (9xxx)
    INTERNAL_ERROR("9001", "Internal server error"),
    UNKNOWN_ERROR("9999", "Unknown error occurred");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
