package com.invoices.search.domain.model;

public record SearchResult(
        String id,
        String title,
        String subtitle,
        String type, // "INVOICE", "CLIENT", "COMPANY"
        String url) {
}
