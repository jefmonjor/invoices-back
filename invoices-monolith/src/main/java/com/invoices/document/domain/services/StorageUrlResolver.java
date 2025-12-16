package com.invoices.document.domain.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service to resolve storage object names to public URLs.
 * Used for displaying logos and other public assets.
 */
@Service
public class StorageUrlResolver {

    @Value("${s3.endpoint}")
    private String s3Endpoint;

    @Value("${s3.bucket-name}")
    private String bucketName;

    @Value("${s3.path-style-access:true}")
    private boolean pathStyleAccess;

    /**
     * Convert an object name (storage key) to a public URL.
     * 
     * For Backblaze B2 with path-style:
     * https://s3.region.backblazeb2.com/bucket/object
     * For virtual-host style: https://bucket.s3.region.backblazeb2.com/object
     */
    public String resolvePublicUrl(String objectName) {
        if (objectName == null || objectName.isEmpty()) {
            return null;
        }

        // If it's already a full URL, return as-is
        if (objectName.startsWith("http://") || objectName.startsWith("https://")) {
            return objectName;
        }

        // Build the URL based on path style
        String normalizedEndpoint = s3Endpoint.endsWith("/")
                ? s3Endpoint.substring(0, s3Endpoint.length() - 1)
                : s3Endpoint;

        if (pathStyleAccess) {
            // Path style: endpoint/bucket/object
            return normalizedEndpoint + "/" + bucketName + "/" + objectName;
        } else {
            // Virtual host style: bucket.endpoint/object
            // Replace schema with bucket prefix
            String withoutSchema = normalizedEndpoint.replaceFirst("^https?://", "");
            String schema = normalizedEndpoint.startsWith("https") ? "https://" : "http://";
            return schema + bucketName + "." + withoutSchema + "/" + objectName;
        }
    }
}
