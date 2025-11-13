package com.invoices.document_service.service;

import com.invoices.document_service.config.MinioConfig;
import com.invoices.document_service.dto.DocumentDTO;
import com.invoices.document_service.dto.UploadDocumentResponse;
import com.invoices.document_service.entity.Document;
import com.invoices.document_service.exception.DocumentNotFoundException;
import com.invoices.document_service.exception.FileUploadException;
import com.invoices.document_service.exception.InvalidFileTypeException;
import com.invoices.document_service.repository.DocumentRepository;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private static final String ALLOWED_CONTENT_TYPE = "application/pdf";
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    private final DocumentRepository documentRepository;
    private final MinioClient minioClient;
    private final MinioConfig.MinioProperties minioProperties;

    public UploadDocumentResponse uploadDocument(MultipartFile file, Long invoiceId, String uploadedBy) {
        log.info("Uploading document: {} for invoice: {}", file.getOriginalFilename(), invoiceId);

        // Validate file
        validateFile(file);

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String uniqueFilename = UUID.randomUUID().toString() + extension;

        try {
            // Upload to MinIO
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(uniqueFilename)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            // Save metadata to database
            Document document = Document.builder()
                    .filename(uniqueFilename)
                    .originalFilename(originalFilename)
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .minioObjectName(uniqueFilename)
                    .invoiceId(invoiceId)
                    .uploadedBy(uploadedBy)
                    .build();

            document = documentRepository.save(document);
            log.info("Document uploaded successfully with ID: {}", document.getId());

            return UploadDocumentResponse.builder()
                    .documentId(document.getId())
                    .filename(originalFilename)
                    .downloadUrl(generateDownloadUrl(document.getId()))
                    .uploadedAt(document.getCreatedAt())
                    .build();

        } catch (Exception e) {
            log.error("Failed to upload document to MinIO", e);
            throw new FileUploadException("Failed to upload document", e);
        }
    }

    @Transactional(readOnly = true)
    public byte[] downloadDocument(Long documentId) {
        log.info("Downloading document with ID: {}", documentId);

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));

        try {
            InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(document.getMinioObjectName())
                            .build()
            );

            byte[] content = stream.readAllBytes();
            stream.close();

            log.info("Document downloaded successfully: {}", documentId);
            return content;

        } catch (Exception e) {
            log.error("Failed to download document from MinIO", e);
            throw new FileUploadException("Failed to download document", e);
        }
    }

    @Transactional(readOnly = true)
    public DocumentDTO getDocumentById(Long id) {
        log.info("Fetching document with ID: {}", id);
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException(id));
        return mapToDTO(document);
    }

    @Transactional(readOnly = true)
    public List<DocumentDTO> getDocumentsByInvoiceId(Long invoiceId) {
        log.info("Fetching documents for invoice ID: {}", invoiceId);
        List<Document> documents = documentRepository.findByInvoiceId(invoiceId);
        return documents.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public void deleteDocument(Long id) {
        log.info("Deleting document with ID: {}", id);

        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException(id));

        try {
            // Delete from MinIO
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(document.getMinioObjectName())
                            .build()
            );

            // Delete from database
            documentRepository.delete(document);
            log.info("Document deleted successfully: {}", id);

        } catch (Exception e) {
            log.error("Failed to delete document from MinIO", e);
            throw new FileUploadException("Failed to delete document", e);
        }
    }

    public String generateDownloadUrl(Long documentId) {
        return "/api/documents/" + documentId + "/download";
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        if (!ALLOWED_CONTENT_TYPE.equals(file.getContentType())) {
            throw new InvalidFileTypeException(file.getContentType());
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum allowed (10MB)");
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".pdf";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    private DocumentDTO mapToDTO(Document document) {
        return DocumentDTO.builder()
                .id(document.getId())
                .filename(document.getFilename())
                .originalFilename(document.getOriginalFilename())
                .contentType(document.getContentType())
                .fileSize(document.getFileSize())
                .invoiceId(document.getInvoiceId())
                .uploadedBy(document.getUploadedBy())
                .createdAt(document.getCreatedAt())
                .downloadUrl(generateDownloadUrl(document.getId()))
                .build();
    }
}
