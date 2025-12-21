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

    List<InvoiceSummary> findSummariesByCompanyId(Long companyId, int page, int size, String search, String status);

    long countByCompanyId(Long companyId);

    long countByCompanyIdAndStatus(Long companyId, String status);

    java.math.BigDecimal sumTotalAmountByCompanyId(Long companyId);

    java.math.BigDecimal sumTotalAmountByCompanyIdAndStatus(Long companyId, String status);

    long countByCompanyId(Long companyId, String search, String status);

    void deleteByCompanyId(Long companyId);

    /**
     * Finds the last created invoice for a company, excluding a specific invoice
     * ID.
     * Used for VeriFactu chaining to find the previous invoice.
     */
    Optional<Invoice> findLastInvoiceByCompanyIdAndIdNot(Long companyId, Long excludedInvoiceId);

    /**
     * Finds the last invoice number for a company and year with pessimistic write
     * lock.
     * Prevents race conditions when multiple threads generate invoice numbers
     * simultaneously.
     * Lock is held for the duration of the transaction.
     *
     * @param companyId the company ID
     * @param year      the year
     * @return the last invoice number, or empty if no invoices exist
     */
    Optional<String> findLastInvoiceNumberByCompanyAndYearWithLock(Long companyId, int year);

    /**
     * Finds all invoices with verifactuStatus in the given list.
     * Used by VerifactuRetryJob to find pending submissions.
     *
     * @param statuses list of VeriFactu statuses to match
     * @return list of invoices with matching statuses
     */
    List<Invoice> findByVerifactuStatusIn(List<String> statuses);

    /**
     * Finds all invoices for a company in a specific quarter.
     * Used for quarterly ZIP download.
     *
     * @param companyId the company ID
     * @param year      the year
     * @param quarter   the quarter (1-4)
     * @return list of invoices for the quarter
     */
    List<Invoice> findByCompanyIdAndQuarter(Long companyId, int year, int quarter);

    /**
     * Finds all invoices for a company in a specific year.
     * Used for full year download.
     *
     * @param companyId the company ID
     * @param year      the year
     * @return list of invoices for the year
     */
    List<Invoice> findByCompanyIdAndYear(Long companyId, int year);
}
