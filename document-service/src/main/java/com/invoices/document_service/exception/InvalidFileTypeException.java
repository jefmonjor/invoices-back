package com.invoices.document_service.exception;

public class InvalidFileTypeException extends RuntimeException {
    public InvalidFileTypeException(String message) {
        super(message);
    }

    public InvalidFileTypeException(String contentType) {
        super("Invalid file type: " + contentType + ". Only PDF files are allowed.");
    }
}
