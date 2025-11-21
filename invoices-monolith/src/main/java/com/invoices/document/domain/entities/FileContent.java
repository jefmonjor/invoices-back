package com.invoices.document.domain.entities;

import java.io.InputStream;
import java.util.Objects;

/**
 * Value object representing file content in the domain layer.
 * This encapsulates the raw file data and its metadata.
 */
public class FileContent {

    public interface InputStreamSupplier {
        InputStream get() throws java.io.IOException;
    }

    private final InputStreamSupplier inputStreamSupplier;
    private final long size;
    private final String contentType;

    public FileContent(InputStreamSupplier inputStreamSupplier, long size, String contentType) {
        if (inputStreamSupplier == null) {
            throw new IllegalArgumentException("InputStreamSupplier cannot be null");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be greater than zero");
        }
        if (contentType == null || contentType.trim().isEmpty()) {
            throw new IllegalArgumentException("Content type cannot be null or empty");
        }

        this.inputStreamSupplier = inputStreamSupplier;
        this.size = size;
        this.contentType = contentType;
    }

    public InputStream getInputStream() {
        try {
            return inputStreamSupplier.get();
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to get input stream", e);
        }
    }

    public long getSize() {
        return size;
    }

    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        FileContent that = (FileContent) o;
        return size == that.size &&
                Objects.equals(contentType, that.contentType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(size, contentType);
    }

    @Override
    public String toString() {
        return "FileContent{" +
                "size=" + size +
                ", contentType='" + contentType + '\'' +
                '}';
    }
}
