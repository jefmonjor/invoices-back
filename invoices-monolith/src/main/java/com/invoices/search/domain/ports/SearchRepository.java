package com.invoices.search.domain.ports;

import com.invoices.search.domain.model.SearchResult;
import java.util.List;

public interface SearchRepository {
    List<SearchResult> searchGlobal(String query, Long companyId);

    List<SearchResult> searchInvoices(String query, Long companyId);

    List<SearchResult> searchClients(String query, Long companyId);

    List<String> getSuggestions(String query, Long companyId);
}
