package com.invoices.search.infrastructure.persistence;

import com.invoices.search.domain.model.SearchResult;
import com.invoices.search.domain.ports.SearchRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class SearchRepositoryImpl implements SearchRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @SuppressWarnings("unchecked")
    public List<SearchResult> searchGlobal(String query, Long companyId) {
        String sql = """
                    SELECT id, invoice_number as title, to_char(issue_date, 'YYYY-MM-DD') as subtitle, 'INVOICE' as type, '/invoices/' || id as url
                    FROM invoices
                    WHERE company_id = :companyId AND search_vector @@ plainto_tsquery('spanish', :query)
                    UNION ALL
                    SELECT id, business_name as title, tax_id as subtitle, 'CLIENT' as type, '/clients/' || id as url
                    FROM clients
                    WHERE company_id = :companyId AND search_vector @@ plainto_tsquery('spanish', :query)
                    LIMIT 20
                """;

        Query nativeQuery = entityManager.createNativeQuery(sql);
        nativeQuery.setParameter("companyId", companyId);
        nativeQuery.setParameter("query", query);

        List<Object[]> results = nativeQuery.getResultList();
        return mapToSearchResults(results);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<SearchResult> searchInvoices(String query, Long companyId) {
        String sql = """
                    SELECT id, invoice_number as title, to_char(issue_date, 'YYYY-MM-DD') as subtitle, 'INVOICE' as type, '/invoices/' || id as url
                    FROM invoices
                    WHERE company_id = :companyId AND search_vector @@ plainto_tsquery('spanish', :query)
                    LIMIT 20
                """;

        Query nativeQuery = entityManager.createNativeQuery(sql);
        nativeQuery.setParameter("companyId", companyId);
        nativeQuery.setParameter("query", query);

        List<Object[]> results = nativeQuery.getResultList();
        return mapToSearchResults(results);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<SearchResult> searchClients(String query, Long companyId) {
        String sql = """
                    SELECT id, business_name as title, tax_id as subtitle, 'CLIENT' as type, '/clients/' || id as url
                    FROM clients
                    WHERE company_id = :companyId AND search_vector @@ plainto_tsquery('spanish', :query)
                    LIMIT 20
                """;

        Query nativeQuery = entityManager.createNativeQuery(sql);
        nativeQuery.setParameter("companyId", companyId);
        nativeQuery.setParameter("query", query);

        List<Object[]> results = nativeQuery.getResultList();
        return mapToSearchResults(results);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> getSuggestions(String query, Long companyId) {
        // Simple implementation for suggestions based on invoice numbers and client
        // names
        String sql = """
                    SELECT invoice_number FROM invoices WHERE company_id = :companyId AND invoice_number ILIKE :query || '%'
                    UNION
                    SELECT business_name FROM clients WHERE company_id = :companyId AND business_name ILIKE :query || '%'
                    LIMIT 5
                """;

        Query nativeQuery = entityManager.createNativeQuery(sql);
        nativeQuery.setParameter("companyId", companyId);
        nativeQuery.setParameter("query", query);

        return nativeQuery.getResultList();
    }

    private List<SearchResult> mapToSearchResults(List<Object[]> results) {
        List<SearchResult> searchResults = new ArrayList<>();
        for (Object[] row : results) {
            searchResults.add(new SearchResult(
                    row[0].toString(),
                    row[1].toString(),
                    row[2] != null ? row[2].toString() : "",
                    row[3].toString(),
                    row[4].toString()));
        }
        return searchResults;
    }
}
