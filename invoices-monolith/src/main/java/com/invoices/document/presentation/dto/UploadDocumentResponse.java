package com.invoices.document.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for document upload operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadDocumentResponse {
    private Long documentId;
    private String filename;
    private String downloadUrl;
    private LocalDateTime uploadedAt;
}
