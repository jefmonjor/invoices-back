package com.invoices.document.presentation.controllers;

import com.invoices.document.domain.ports.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;

/**
 * Controller to proxy file access from S3 storage.
 * This avoids CORS issues by serving files through the backend.
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "File Access", description = "Endpoints for accessing stored files")
public class FileController {

    private final FileStorageService fileStorageService;

    @GetMapping("/logos/{objectName}")
    @Operation(summary = "Get company logo", description = "Retrieves a company logo from storage. Public endpoint for PDF generation.", responses = {
            @ApiResponse(responseCode = "200", description = "Logo retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Logo not found")
    })
    public ResponseEntity<InputStreamResource> getLogo(@PathVariable String objectName) {
        String fullPath = "logos/" + objectName;
        log.info("Serving logo: {}", fullPath);

        try {
            InputStream inputStream = fileStorageService.retrieveFile(fullPath);
            if (inputStream == null) {
                log.warn("Logo not found: {}", fullPath);
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + objectName + "\"")
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400") // Cache for 24 hours
                    .contentType(MediaType.IMAGE_PNG)
                    .body(new InputStreamResource(inputStream));
        } catch (Exception e) {
            log.error("Error serving logo: {}", fullPath, e);
            return ResponseEntity.notFound().build();
        }
    }
}
