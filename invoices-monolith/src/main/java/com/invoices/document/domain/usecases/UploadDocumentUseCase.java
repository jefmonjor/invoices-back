package com.invoices.document.domain.usecases;

import com.invoices.document.domain.entities.Document;
import com.invoices.document.domain.entities.FileContent;
import com.invoices.document.domain.ports.DocumentRepository;
import com.invoices.document.domain.ports.FileStorageService;
import com.invoices.document.domain.validation.PdfValidator;

import java.nio.file.Paths;
import java.util.UUID;

/**
 * Use Case for uploading a document.
 * Encapsulates the business logic for uploading and storing a PDF document.
 */
public class UploadDocumentUseCase {

    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final PdfValidator pdfValidator;
    private final com.invoices.invoice.domain.ports.InvoiceRepository invoiceRepository;

    public UploadDocumentUseCase(
            DocumentRepository documentRepository,
            FileStorageService fileStorageService,
            PdfValidator pdfValidator,
            com.invoices.invoice.domain.ports.InvoiceRepository invoiceRepository) {
        this.documentRepository = documentRepository;
        this.fileStorageService = fileStorageService;
        this.pdfValidator = pdfValidator;
        this.invoiceRepository = invoiceRepository;
    }

    /**
     * Executes the upload document use case.
     *
     * @param fileContent      the file content to upload
     * @param originalFilename the original filename (ignored if invoiceId is
     *                         present)
     * @param invoiceId        optional invoice ID to associate with the document
     * @param uploadedBy       username of the uploader
     * @return the created Document entity
     */
    public Document execute(
            FileContent fileContent,
            String originalFilename,
            Long invoiceId,
            String uploadedBy) {
        // Validate the PDF file
        pdfValidator.validate(fileContent);

        // Determine filename based on invoice data if available
        String finalFilename = originalFilename;
        if (invoiceId != null) {
            com.invoices.invoice.domain.entities.Invoice invoice = invoiceRepository.findById(invoiceId)
                    .orElse(null);

            if (invoice != null) {
                finalFilename = generateFilename(invoice);
            }
        }

        // Generate unique storage object name
        String extension = ".pdf"; // Enforced by generateFilename or validation
        if (!finalFilename.toLowerCase().endsWith(".pdf")) {
            // Fallback if originalFilename was passed and didn't have extension (unlikely)
            extension = extractFileExtension(finalFilename);
        }

        String uniqueFilename = UUID.randomUUID().toString() + extension;

        // Store file in storage service (MinIO, S3, etc.)
        fileStorageService.storeFile(uniqueFilename, fileContent);

        // Create domain entity
        Document document = new Document(
                uniqueFilename, // filename (storage key)
                finalFilename, // originalFilename (display name)
                fileContent.getContentType(), // contentType
                fileContent.getSize(), // fileSize
                uniqueFilename, // storageObjectName
                invoiceId, // invoiceId (can be null)
                uploadedBy != null ? uploadedBy : "system" // uploadedBy
        );

        // Save metadata to repository
        return documentRepository.save(document);
    }

    private String generateFilename(com.invoices.invoice.domain.entities.Invoice invoice) {
        // Check for vehicle plate in items
        String plate = invoice.getItems().stream()
                .map(com.invoices.invoice.domain.entities.InvoiceItem::getVehiclePlate)
                .filter(p -> p != null && !p.trim().isEmpty())
                .findFirst()
                .orElse(null);

        if (plate != null) {
            // Format: PLATE-MONTH-YEAR.pdf
            String month = getSpanishMonthAbbreviation(invoice.getIssueDate().getMonthValue());
            int year = invoice.getIssueDate().getYear();
            return String.format("%s-%s-%d.pdf", plate.toUpperCase(), month, year);
        } else {
            // Format: FacturaXXX.pdf
            // Extract number part from invoice number (e.g., 001/2025 -> 001)
            String invoiceNumber = invoice.getInvoiceNumber();
            String numberPart = invoiceNumber;
            if (invoiceNumber != null && invoiceNumber.contains("/")) {
                numberPart = invoiceNumber.split("/")[0];
            }
            return String.format("Factura%s.pdf", numberPart);
        }
    }

    private String getSpanishMonthAbbreviation(int month) {
        String[] months = { "ENE", "FEB", "MAR", "ABR", "MAY", "JUN", "JUL", "AGO", "SEP", "OCT", "NOV", "DIC" };
        if (month >= 1 && month <= 12) {
            return months[month - 1];
        }
        return "UNK";
    }

    /**
     * Extracts and validates the file extension from a filename.
     * Sanitizes the filename to prevent path traversal attacks.
     * Only allows .pdf extension.
     *
     * @param filename the original filename
     * @return the sanitized .pdf extension
     * @throws IllegalArgumentException if filename is invalid or extension is not
     *                                  .pdf
     */
    private String extractFileExtension(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be null or empty");
        }

        // Sanitize filename - remove any path components to prevent path traversal
        // This prevents filenames like "../../etc/passwd" or
        // "C:\Windows\system32\file.pdf"
        String sanitizedFilename = Paths.get(filename).getFileName().toString();

        // Remove any remaining dangerous characters
        sanitizedFilename = sanitizedFilename.replaceAll("[^a-zA-Z0-9._-]", "_");

        // Extract extension
        int lastDot = sanitizedFilename.lastIndexOf('.');
        if (lastDot == -1 || lastDot == sanitizedFilename.length() - 1) {
            throw new IllegalArgumentException("Filename must have a valid .pdf extension");
        }

        String extension = sanitizedFilename.substring(lastDot).toLowerCase();

        // Validate that extension is .pdf
        if (!".pdf".equals(extension)) {
            throw new IllegalArgumentException("Only PDF files are allowed. Found extension: " + extension);
        }

        return extension;
    }
}
