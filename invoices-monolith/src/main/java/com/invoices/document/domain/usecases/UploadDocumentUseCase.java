package com.invoices.document.domain.usecases;

import com.invoices.document.domain.entities.Document;
import com.invoices.document.domain.entities.FileContent;
import com.invoices.document.domain.ports.DocumentRepository;
import com.invoices.document.domain.ports.FileStorageService;
import com.invoices.document.domain.validation.PdfValidator;

import java.util.UUID;

/**
 * Use Case for uploading a document.
 * Encapsulates the business logic for uploading and storing a PDF document.
 */
public class UploadDocumentUseCase {

    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final PdfValidator pdfValidator;

    public UploadDocumentUseCase(
            DocumentRepository documentRepository,
            FileStorageService fileStorageService,
            PdfValidator pdfValidator
    ) {
        this.documentRepository = documentRepository;
        this.fileStorageService = fileStorageService;
        this.pdfValidator = pdfValidator;
    }

    /**
     * Executes the upload document use case.
     *
     * @param fileContent      the file content to upload
     * @param originalFilename the original filename
     * @param invoiceId        optional invoice ID to associate with the document
     * @param uploadedBy       username of the uploader
     * @return the created Document entity
     */
    public Document execute(
            FileContent fileContent,
            String originalFilename,
            Long invoiceId,
            String uploadedBy
    ) {
        // Validate the PDF file
        pdfValidator.validate(fileContent);

        // Generate unique storage object name
        String extension = extractFileExtension(originalFilename);
        String uniqueFilename = UUID.randomUUID().toString() + extension;

        // Store file in storage service (MinIO, S3, etc.)
        fileStorageService.storeFile(uniqueFilename, fileContent);

        // Create domain entity
        Document document = new Document(
                uniqueFilename,              // filename
                originalFilename,            // originalFilename
                fileContent.getContentType(), // contentType
                fileContent.getSize(),       // fileSize
                uniqueFilename,              // storageObjectName
                invoiceId,                   // invoiceId (can be null)
                uploadedBy != null ? uploadedBy : "system" // uploadedBy
        );

        // Save metadata to repository
        return documentRepository.save(document);
    }

    /**
     * Extracts the file extension from a filename.
     */
    private String extractFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".pdf";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}
