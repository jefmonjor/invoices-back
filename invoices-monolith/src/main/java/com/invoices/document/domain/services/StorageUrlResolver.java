package com.invoices.document.domain.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service to resolve storage object names to accessible URLs.
 * Returns backend proxy URLs to avoid CORS issues with direct S3 access.
 */
@Service
@Slf4j
public class StorageUrlResolver {

    @Value("${app.backend-url:}")
    private String backendUrl;

    /**
     * Convert an object name (storage key) to a proxy URL.
     * The URL points to the backend file proxy endpoint.
     * 
     * @param objectName The object name in storage (e.g.,
     *                   "logos/company-7-xxx.png")
     * @return A URL to the backend proxy endpoint
     */
    public String resolvePublicUrl(String objectName) {
        if (objectName == null || objectName.isEmpty()) {
            return null;
        }

        // If it's already a full URL, return as-is
        if (objectName.startsWith("http://") || objectName.startsWith("https://")) {
            return objectName;
        }

        // Build proxy URL through backend
        // For logos: logos/company-7-xxx.png -> /api/files/logos/company-7-xxx.png
        String baseUrl = backendUrl != null && !backendUrl.isEmpty()
                ? backendUrl
                : "";

        String proxyUrl = baseUrl + "/api/files/" + objectName;
        log.debug("Resolved logo URL: {} -> {}", objectName, proxyUrl);
        return proxyUrl;
    }
}
