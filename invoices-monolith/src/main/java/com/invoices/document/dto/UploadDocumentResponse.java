package com.invoices.document_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
