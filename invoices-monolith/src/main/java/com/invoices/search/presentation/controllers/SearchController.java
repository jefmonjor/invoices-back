package com.invoices.search.presentation.controllers;

import com.invoices.search.application.services.SearchService;
import com.invoices.search.domain.model.SearchResult;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    @PreAuthorize("@companySecurity.hasCompanyAccess(#companyId)")
    public ResponseEntity<List<SearchResult>> searchGlobal(
            @RequestParam String query,
            @RequestParam Long companyId) {
        return ResponseEntity.ok(searchService.searchGlobal(query, companyId));
    }

    @GetMapping("/invoices")
    @PreAuthorize("@companySecurity.hasCompanyAccess(#companyId)")
    public ResponseEntity<List<SearchResult>> searchInvoices(
            @RequestParam String query,
            @RequestParam Long companyId) {
        return ResponseEntity.ok(searchService.searchInvoices(query, companyId));
    }

    @GetMapping("/clients")
    @PreAuthorize("@companySecurity.hasCompanyAccess(#companyId)")
    public ResponseEntity<List<SearchResult>> searchClients(
            @RequestParam String query,
            @RequestParam Long companyId) {
        return ResponseEntity.ok(searchService.searchClients(query, companyId));
    }

    @GetMapping("/suggestions")
    @PreAuthorize("@companySecurity.hasCompanyAccess(#companyId)")
    public ResponseEntity<List<String>> getSuggestions(
            @RequestParam String query,
            @RequestParam Long companyId) {
        return ResponseEntity.ok(searchService.getSuggestions(query, companyId));
    }
}
