package com.invoices.invoice.infrastructure.persistence.repositories;

import com.invoices.invoice.domain.ports.VerifactuMetricsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Implementation of VerifactuMetricsRepository using optimized JPA queries.
 *
 * Encapsulates complex metrics queries to avoid N+1 and bulk data loading.
 * All queries are optimized for aggregation and counting.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class VerifactuMetricsRepositoryImpl implements VerifactuMetricsRepository {

    private final JpaInvoiceRepository jpaRepository;

    @Override
    public Long countByVerifactuStatusAndCreatedAtAfter(String status, LocalDateTime after) {
        log.debug("Counting invoices with status {} created after {}", status, after);
        return jpaRepository.countByVerifactuStatusAndCreatedAtAfter(status, after);
    }

    @Override
    public Long countByVerifactuStatusIn(List<String> statuses) {
        log.debug("Counting invoices with statuses in {}", statuses);
        return jpaRepository.countByVerifactuStatusIn(statuses);
    }

    @Override
    public Long countByCreatedAtAfter(LocalDateTime after) {
        log.debug("Counting invoices created after {}", after);
        return jpaRepository.countByCreatedAtAfter(after);
    }

    @Override
    public Long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to) {
        log.debug("Counting invoices created between {} and {}", from, to);
        return jpaRepository.countByCreatedAtBetween(from, to);
    }

    @Override
    public Long countByVerifactuStatusAndCreatedAtBetween(String status, LocalDateTime from, LocalDateTime to) {
        log.debug("Counting invoices with status {} created between {} and {}", status, from, to);
        return jpaRepository.countByVerifactuStatusAndCreatedAtBetween(status, from, to);
    }

    @Override
    public Long countAll() {
        log.debug("Counting all invoices");
        return jpaRepository.count();
    }

    @Override
    public Map<String, Long> getErrorDistribution(LocalDateTime from, LocalDateTime to) {
        log.debug("Getting error distribution between {} and {}", from, to);

        Map<String, Long> distribution = new HashMap<>();

        // Count invoices by each error status
        Long rejected = jpaRepository.countByVerifactuStatusAndCreatedAtBetween("REJECTED", from, to);
        Long failed = jpaRepository.countByVerifactuStatusAndCreatedAtBetween("FAILED", from, to);
        Long timeout = jpaRepository.countByVerifactuStatusAndCreatedAtBetween("TIMEOUT", from, to);

        // Add to map only if count > 0
        if (rejected > 0) {
            distribution.put("REJECTED", rejected);
        }
        if (failed > 0) {
            distribution.put("FAILED", failed);
        }
        if (timeout > 0) {
            distribution.put("TIMEOUT", timeout);
        }

        return distribution;
    }
}
