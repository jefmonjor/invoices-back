package com.invoices.document.infrastructure.config;

import com.invoices.document.domain.ports.DocumentRepository;
import com.invoices.document.domain.ports.FileStorageService;
import com.invoices.document.domain.usecases.*;
import com.invoices.document.domain.validation.PdfValidator;
import org.apache.tika.Tika;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for Document use cases.
 * This wires up all use cases with their dependencies (ports).
 */
@Configuration
public class DocumentUseCaseConfiguration {

    /**
     * Create a singleton Tika instance to avoid repeated instantiation.
     * Tika is a heavyweight object that performs file type detection.
     * Reusing a single instance improves performance and reduces memory consumption.
     *
     * @return singleton Tika instance
     */
    @Bean
    public Tika tika() {
        return new Tika();
    }

    @Bean
    public PdfValidator pdfValidator(Tika tika) {
        return new PdfValidator(tika);
    }

    @Bean
    public UploadDocumentUseCase uploadDocumentUseCase(
            DocumentRepository documentRepository,
            FileStorageService fileStorageService,
            PdfValidator pdfValidator,
            com.invoices.invoice.domain.ports.InvoiceRepository invoiceRepository,
            com.invoices.document.domain.services.StorageKeyGenerator storageKeyGenerator) {
        return new UploadDocumentUseCase(documentRepository, fileStorageService, pdfValidator, invoiceRepository,
                storageKeyGenerator);
    }

    @Bean
    public DownloadDocumentUseCase downloadDocumentUseCase(
            DocumentRepository documentRepository,
            FileStorageService fileStorageService) {
        return new DownloadDocumentUseCase(documentRepository, fileStorageService);
    }

    @Bean
    public GetDocumentByIdUseCase getDocumentByIdUseCase(
            DocumentRepository documentRepository) {
        return new GetDocumentByIdUseCase(documentRepository);
    }

    @Bean
    public GetDocumentsByInvoiceUseCase getDocumentsByInvoiceUseCase(
            DocumentRepository documentRepository) {
        return new GetDocumentsByInvoiceUseCase(documentRepository);
    }

    @Bean
    public DeleteDocumentUseCase deleteDocumentUseCase(
            DocumentRepository documentRepository,
            FileStorageService fileStorageService) {
        return new DeleteDocumentUseCase(documentRepository, fileStorageService);
    }
}
