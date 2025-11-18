package com.invoices.document.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for Document presentation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDTO {
    private Long id;
    private String filename;
    private String originalFilename;
    private String contentType;
    private Long fileSize;
    private Long invoiceId;
    private String uploadedBy;
    private LocalDateTime createdAt;
    private String downloadUrl;
}
