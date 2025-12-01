package com.invoices.search.application.services;

import com.invoices.search.domain.model.SearchResult;
import com.invoices.search.domain.ports.SearchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class SearchService {

    private final SearchRepository searchRepository;

    public SearchService(SearchRepository searchRepository) {
        this.searchRepository = searchRepository;
    }

    public List<SearchResult> searchGlobal(String query, Long companyId) {
        return searchRepository.searchGlobal(query, companyId);
    }

    public List<SearchResult> searchInvoices(String query, Long companyId) {
        return searchRepository.searchInvoices(query, companyId);
    }

    public List<SearchResult> searchClients(String query, Long companyId) {
        return searchRepository.searchClients(query, companyId);
    }

    public List<String> getSuggestions(String query, Long companyId) {
        return searchRepository.getSuggestions(query, companyId);
    }
}
