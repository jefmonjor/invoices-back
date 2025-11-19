package com.invoices.document.domain.validation;

import com.invoices.document.domain.entities.FileContent;
import org.apache.tika.Tika;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Domain service for PDF file validation.
 * This encapsulates all validation logic for PDF files without framework dependencies.
 */
public class PdfValidator {

    private static final String ALLOWED_CONTENT_TYPE = "application/pdf";
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final String PDF_SIGNATURE = "%PDF-";
    private static final int PDF_HEADER_SIZE = 5;

    private final Tika tika;

    public PdfValidator() {
        this.tika = new Tika();
    }

    /**
     * Validates a file to ensure it's a valid PDF and meets size requirements.
     *
     * @param fileContent the file content to validate
     * @throws IllegalArgumentException if validation fails
     */
    public void validate(FileContent fileContent) {
        validateNotNull(fileContent);
        validateFileSize(fileContent.getSize());
        validateDeclaredContentType(fileContent.getContentType());
        validateActualContentType(fileContent);
        validatePdfSignature(fileContent);
    }

    /**
     * Validates that file content is not null.
     */
    private void validateNotNull(FileContent fileContent) {
        if (fileContent == null || fileContent.getInputStream() == null) {
            throw new IllegalArgumentException("File content cannot be null or empty");
        }
    }

    /**
     * Validates that file size does not exceed the maximum allowed size.
     */
    private void validateFileSize(long fileSize) {
        if (fileSize > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    String.format("File size exceeds maximum allowed (10MB). Size: %.2f MB",
                            fileSize / (1024.0 * 1024.0))
            );
        }
    }

    /**
     * Validates the declared content type.
     */
    private void validateDeclaredContentType(String contentType) {
        if (!ALLOWED_CONTENT_TYPE.equals(contentType)) {
            throw new IllegalArgumentException(
                    "Invalid file type: " + contentType + ". Only PDF files are allowed."
            );
        }
    }

    /**
     * Validates the actual file content using Apache Tika to detect the real MIME type.
     * This prevents fake PDF files (e.g., renamed .txt files).
     */
    private void validateActualContentType(FileContent fileContent) {
        try {
            InputStream originalStream = fileContent.getInputStream();

            // Wrap in BufferedInputStream to guarantee mark/reset support
            // BufferedInputStream provides efficient mark/reset functionality
            BufferedInputStream bufferedStream = new BufferedInputStream(originalStream);

            // Mark the stream with reasonable limit (enough for Tika detection)
            // Use file size or a reasonable default (5MB) for complex PDFs
            // 5MB should be sufficient for most PDF magic byte detection
            int markLimit = (int) Math.min(fileContent.getSize(), 5 * 1024 * 1024);
            bufferedStream.mark(markLimit);

            String detectedType = tika.detect(bufferedStream);

            // Reset stream to beginning for subsequent validations
            bufferedStream.reset();

            if (!ALLOWED_CONTENT_TYPE.equals(detectedType)) {
                throw new IllegalArgumentException(
                        "File content does not match PDF format. Detected type: " + detectedType
                );
            }

        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to validate file content: " + e.getMessage(), e);
        }
    }

    /**
     * Validates the PDF file signature (magic bytes).
     * All valid PDF files must start with "%PDF-".
     */
    private void validatePdfSignature(FileContent fileContent) {
        try {
            InputStream originalStream = fileContent.getInputStream();

            // Wrap in BufferedInputStream to guarantee mark/reset support
            BufferedInputStream bufferedStream = new BufferedInputStream(originalStream);

            // Mark the stream so we can reset it after reading header
            bufferedStream.mark(PDF_HEADER_SIZE);

            byte[] header = new byte[PDF_HEADER_SIZE];
            int bytesRead = bufferedStream.read(header);

            // Reset stream to beginning
            bufferedStream.reset();

            if (bytesRead < PDF_HEADER_SIZE) {
                throw new IllegalArgumentException("File is too small to be a valid PDF");
            }

            String headerString = new String(header);
            if (!headerString.startsWith(PDF_SIGNATURE)) {
                throw new IllegalArgumentException(
                        "File does not have a valid PDF signature. Expected: " + PDF_SIGNATURE
                );
            }

        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read PDF signature: " + e.getMessage(), e);
        }
    }

    /**
     * Gets the maximum allowed file size.
     */
    public long getMaxFileSize() {
        return MAX_FILE_SIZE;
    }

    /**
     * Gets the allowed content type.
     */
    public String getAllowedContentType() {
        return ALLOWED_CONTENT_TYPE;
    }
}
