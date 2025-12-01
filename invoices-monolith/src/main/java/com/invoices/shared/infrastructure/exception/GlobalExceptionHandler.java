package com.invoices.shared.infrastructure.exception;

import com.invoices.shared.domain.exception.BusinessException;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

        @ExceptionHandler(BusinessException.class)
        public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
                ErrorResponse error = ErrorResponse.builder()
                                .code(e.getCode())
                                .message(e.getMessage())
                                .timestamp(Instant.now())
                                .traceId(MDC.get("traceId"))
                                .build();

                return ResponseEntity.status(e.getStatus()).body(error);
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
                Map<String, String> fieldErrors = e.getBindingResult()
                                .getFieldErrors().stream()
                                .collect(Collectors.toMap(
                                                FieldError::getField,
                                                FieldError::getDefaultMessage,
                                                (v1, v2) -> v1 + ", " + v2));

                return ResponseEntity.badRequest().body(
                                ErrorResponse.withFieldErrors(fieldErrors));
        }

        @ExceptionHandler(ValidationException.class)
        public ResponseEntity<ErrorResponse> handleValidationException(ValidationException e) {
                return ResponseEntity.badRequest().body(
                                ErrorResponse.builder()
                                                .code("VALIDATION_ERROR")
                                                .message(e.getMessage())
                                                .timestamp(Instant.now())
                                                .build());
        }

        // ==================== USER MODULE EXCEPTIONS ====================

        @ExceptionHandler(com.invoices.user.exception.UserNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleUserNotFound(com.invoices.user.exception.UserNotFoundException e) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                                ErrorResponse.builder()
                                                .code("USER_NOT_FOUND")
                                                .message(e.getMessage())
                                                .timestamp(Instant.now())
                                                .build());
        }

        @ExceptionHandler(com.invoices.user.exception.UserAlreadyExistsException.class)
        public ResponseEntity<ErrorResponse> handleUserAlreadyExists(
                        com.invoices.user.exception.UserAlreadyExistsException e) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                                ErrorResponse.builder()
                                                .code("USER_ALREADY_EXISTS")
                                                .message(e.getMessage())
                                                .timestamp(Instant.now())
                                                .build());
        }

        @ExceptionHandler({ com.invoices.user.exception.InvalidCredentialsException.class,
                        org.springframework.security.authentication.BadCredentialsException.class })
        public ResponseEntity<ErrorResponse> handleInvalidCredentials(Exception e) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                                ErrorResponse.builder()
                                                .code("INVALID_CREDENTIALS")
                                                .message("Invalid email or password")
                                                .timestamp(Instant.now())
                                                .build());
        }

        @ExceptionHandler(com.invoices.user.exception.InvalidTokenException.class)
        public ResponseEntity<ErrorResponse> handleInvalidToken(com.invoices.user.exception.InvalidTokenException e) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                                ErrorResponse.builder()
                                                .code("INVALID_TOKEN")
                                                .message(e.getMessage())
                                                .timestamp(Instant.now())
                                                .build());
        }

        @ExceptionHandler(com.invoices.user.exception.TokenExpiredException.class)
        public ResponseEntity<ErrorResponse> handleTokenExpired(com.invoices.user.exception.TokenExpiredException e) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                                ErrorResponse.builder()
                                                .code("TOKEN_EXPIRED")
                                                .message("Authentication token has expired")
                                                .timestamp(Instant.now())
                                                .build());
        }

        @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
        public ResponseEntity<ErrorResponse> handleAccessDenied(
                        org.springframework.security.access.AccessDeniedException e) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                                ErrorResponse.builder()
                                                .code("ACCESS_DENIED")
                                                .message("You don't have permission to access this resource")
                                                .timestamp(Instant.now())
                                                .build());
        }

        // ==================== INVOICE MODULE EXCEPTIONS ====================

        @ExceptionHandler(com.invoices.invoice.domain.exceptions.InvoiceNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleInvoiceNotFound(
                        com.invoices.invoice.domain.exceptions.InvoiceNotFoundException e) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                                ErrorResponse.builder()
                                                .code("INVOICE_NOT_FOUND")
                                                .message(e.getMessage())
                                                .timestamp(Instant.now())
                                                .build());
        }

        @ExceptionHandler(com.invoices.invoice.domain.exceptions.ClientNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleClientNotFound(
                        com.invoices.invoice.domain.exceptions.ClientNotFoundException e) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                                ErrorResponse.builder()
                                                .code("CLIENT_NOT_FOUND")
                                                .message(e.getMessage())
                                                .timestamp(Instant.now())
                                                .build());
        }

        @ExceptionHandler(com.invoices.invoice.domain.exceptions.InvalidInvoiceStateException.class)
        public ResponseEntity<ErrorResponse> handleInvalidInvoiceState(
                        com.invoices.invoice.domain.exceptions.InvalidInvoiceStateException e) {
                return ResponseEntity.badRequest().body(
                                ErrorResponse.builder()
                                                .code("INVALID_INVOICE_STATE")
                                                .message(e.getMessage())
                                                .timestamp(Instant.now())
                                                .build());
        }

        @ExceptionHandler(com.invoices.invoice.domain.exceptions.InvalidInvoiceNumberFormatException.class)
        public ResponseEntity<ErrorResponse> handleInvalidInvoiceNumberFormat(
                        com.invoices.invoice.domain.exceptions.InvalidInvoiceNumberFormatException e) {
                return ResponseEntity.badRequest().body(
                                ErrorResponse.builder()
                                                .code("INVALID_INVOICE_NUMBER_FORMAT")
                                                .message(e.getMessage())
                                                .timestamp(Instant.now())
                                                .build());
        }

        // ==================== DOCUMENT MODULE EXCEPTIONS ====================

        @ExceptionHandler(com.invoices.document.exception.DocumentNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleDocumentNotFound(
                        com.invoices.document.exception.DocumentNotFoundException e) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                                ErrorResponse.builder()
                                                .code("DOCUMENT_NOT_FOUND")
                                                .message(e.getMessage())
                                                .timestamp(Instant.now())
                                                .build());
        }

        @ExceptionHandler(com.invoices.document.exception.FileUploadException.class)
        public ResponseEntity<ErrorResponse> handleFileUpload(com.invoices.document.exception.FileUploadException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                                ErrorResponse.builder()
                                                .code("FILE_UPLOAD_ERROR")
                                                .message(e.getMessage())
                                                .timestamp(Instant.now())
                                                .build());
        }

        @ExceptionHandler(com.invoices.document.exception.InvalidFileTypeException.class)
        public ResponseEntity<ErrorResponse> handleInvalidFileType(
                        com.invoices.document.exception.InvalidFileTypeException e) {
                return ResponseEntity.badRequest().body(
                                ErrorResponse.builder()
                                                .code("INVALID_FILE_TYPE")
                                                .message(e.getMessage())
                                                .timestamp(Instant.now())
                                                .build());
        }

        @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
        public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(
                        org.springframework.web.multipart.MaxUploadSizeExceededException e) {
                return ResponseEntity.badRequest().body(
                                ErrorResponse.builder()
                                                .code("MAX_UPLOAD_SIZE_EXCEEDED")
                                                .message("File size exceeds maximum allowed")
                                                .timestamp(Instant.now())
                                                .build());
        }

        // ==================== TRACE MODULE EXCEPTIONS ====================

        @ExceptionHandler(com.invoices.trace.exception.AuditLogNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleAuditLogNotFound(
                        com.invoices.trace.exception.AuditLogNotFoundException e) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                                ErrorResponse.builder()
                                                .code("AUDIT_LOG_NOT_FOUND")
                                                .message(e.getMessage())
                                                .timestamp(Instant.now())
                                                .build());
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleGeneric(Exception e) {
                log.error("Unhandled exception", e);

                // Secure default message for production
                String message = "An unexpected error occurred. Please contact support.";

                return ResponseEntity.internalServerError().body(
                                ErrorResponse.generic(message));
        }
}
