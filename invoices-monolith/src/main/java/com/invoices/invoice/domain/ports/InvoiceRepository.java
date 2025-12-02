package com.invoices.invoice.domain.ports;

import com.invoices.invoice.domain.entities.Invoice;
import com.invoices.invoice.domain.models.InvoiceSummary;

import java.util.List;
import java.util.Optional;

/**
 * Port (interface) for invoice persistence.
 * Domain layer defines the contract, infrastructure implements it.
 * Dependency Inversion Principle (SOLID).
 */
public interface InvoiceRepository {

    Optional<Invoice> findById(Long id);

    List<Invoice> findByUserId(Long userId);

    List<Invoice> findAll();

    Invoice save(Invoice invoice);

    void delete(Invoice invoice);

    void deleteById(Long id);

    boolean existsById(Long id);

    Optional<String> findLastInvoiceNumberByYear(int year);

    Optional<String> findLastInvoiceNumberByCompanyAndYear(Long companyId, int year);

    List<Invoice> findByCompanyId(Long companyId);

    List<InvoiceSummary> findSummariesByCompanyId(Long companyId);

    // Pagination support
    List<InvoiceSummary> findSummariesByCompanyId(Long companyId, int page, int size);

    long countByCompanyId(Long companyId);

    void deleteByCompanyId(Long companyId);

    /**
     * Finds the last created invoice for a company, excluding a specific invoice
     * ID.
     * Used for VeriFactu chaining to find the previous invoice.
     */
    Optional<Invoice> findLastInvoiceByCompanyIdAndIdNot(Long companyId, Long excludedInvoiceId);

    /**
     * Finds the last invoice number for a company and year with pessimistic write lock.
     * Prevents race conditions when multiple threads generate invoice numbers simultaneously.
     * Lock is held for the duration of the transaction.
     *
     * @param companyId the company ID
     * @param year the year
     * @return the last invoice number, or empty if no invoices exist
     */
    Optional<String> findLastInvoiceNumberByCompanyAndYearWithLock(Long companyId, int year);
}
