package com.invoices.invoice.infrastructure.persistence.repositories;

import com.invoices.invoice.infrastructure.persistence.entities.InvoiceJpaEntity;
import com.invoices.invoice.infrastructure.persistence.projections.InvoiceSummaryView;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository interface.
 * Infrastructure layer - persistence mechanism.
 * No @Repository needed - Spring Data JPA auto-detects this interface.
 */
public interface JpaInvoiceRepository extends JpaRepository<InvoiceJpaEntity, Long> {

        List<InvoiceJpaEntity> findByUserId(Long userId);

        List<InvoiceJpaEntity> findByCompanyId(Long companyId);

        List<InvoiceSummaryView> findProjectedByCompanyId(Long companyId);

        org.springframework.data.domain.Page<InvoiceSummaryView> findProjectedByCompanyId(Long companyId,
                        org.springframework.data.domain.Pageable pageable);

        boolean existsByInvoiceNumber(String invoiceNumber);

        @org.springframework.data.jpa.repository.Query("SELECT i.invoiceNumber FROM InvoiceJpaEntity i WHERE YEAR(i.issueDate) = :year ORDER BY i.invoiceNumber DESC LIMIT 1")
        java.util.Optional<String> findLastInvoiceNumberByYear(
                        @org.springframework.data.repository.query.Param("year") int year);

        @org.springframework.data.jpa.repository.Query("SELECT i.invoiceNumber FROM InvoiceJpaEntity i WHERE i.companyId = :companyId AND YEAR(i.issueDate) = :year ORDER BY i.invoiceNumber DESC LIMIT 1")
        java.util.Optional<String> findLastInvoiceNumberByCompanyAndYear(
                        @org.springframework.data.repository.query.Param("companyId") Long companyId,
                        @org.springframework.data.repository.query.Param("year") int year);

        /**
         * Finds the last invoice number for a company and year with pessimistic write lock.
         * Prevents race conditions when multiple threads generate invoice numbers simultaneously.
         * Lock is held for the duration of the transaction.
         *
         * @param companyId the company ID
         * @param year the year
         * @return the last invoice number, or empty if no invoices exist
         */
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT i.invoiceNumber FROM InvoiceJpaEntity i WHERE i.companyId = :companyId AND YEAR(i.issueDate) = :year ORDER BY i.invoiceNumber DESC LIMIT 1")
        Optional<String> findLastInvoiceNumberByCompanyAndYearWithLock(
                        @Param("companyId") Long companyId,
                        @Param("year") int year);

        // VeriFactu query methods for batch scheduler and metrics
        List<InvoiceJpaEntity> findByVerifactuStatusInAndUpdatedAtBefore(
                        List<String> statuses, LocalDateTime updatedBefore);

        Long countByCreatedAtAfter(LocalDateTime createdAfter);

        Long countByVerifactuStatusAndCreatedAtAfter(
                        String verifactuStatus, LocalDateTime createdAfter);

        Long countByVerifactuStatusIn(List<String> statuses);

        Long countByCreatedAtBetween(
                        LocalDateTime start, LocalDateTime end);

        Long countByVerifactuStatusAndCreatedAtBetween(
                        String verifactuStatus, LocalDateTime start, LocalDateTime end);

        Long countByVerifactuStatusAndUpdatedAtBefore(
                        String verifactuStatus, LocalDateTime updatedBefore);

        long countByCompanyId(Long companyId);

        void deleteByCompanyId(Long companyId);

        /**
         * Finds the last created invoice for a company, excluding a specific invoice
         * ID.
         */
        java.util.Optional<InvoiceJpaEntity> findFirstByCompanyIdAndIdNotOrderByCreatedAtDesc(Long companyId,
                        Long excludedId);
}
