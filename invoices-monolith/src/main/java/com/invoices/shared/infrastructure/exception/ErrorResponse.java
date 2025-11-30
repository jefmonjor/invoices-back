package com.invoices.shared.infrastructure.exception;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
public class ErrorResponse {
    private String code;
    private String message;
    private Instant timestamp;
    private String traceId;
    private Map<String, String> fieldErrors;

    public static ErrorResponse generic(String message) {
        return ErrorResponse.builder()
                .code("INTERNAL_ERROR")
                .message(message)
                .timestamp(Instant.now())
                .build();
    }

    public static ErrorResponse withFieldErrors(Map<String, String> fieldErrors) {
        return ErrorResponse.builder()
                .code("VALIDATION_ERROR")
                .message("Validation failed")
                .timestamp(Instant.now())
                .fieldErrors(fieldErrors)
                .build();
    }
}
