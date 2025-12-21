package com.invoices.invoice.presentation.controllers;

import com.invoices.invoice.domain.entities.Client;
import com.invoices.invoice.domain.entities.Company;
import com.invoices.invoice.domain.entities.Invoice;
import com.invoices.invoice.domain.ports.ClientRepository;
import com.invoices.invoice.domain.ports.CompanyRepository;
import com.invoices.invoice.domain.ports.InvoiceRepository;
import com.invoices.invoice.infrastructure.persistence.repositories.JpaInvoiceRepository;
import com.invoices.invoice.infrastructure.services.ExcelExportService;
import com.invoices.invoice.infrastructure.services.InvoiceZipService;
import com.invoices.security.context.CompanyContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

/**
 * REST controller for invoice export operations.
 * Handles quarterly downloads, ZIP generation, and Excel exports.
 */
@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("!hasRole('PLATFORM_ADMIN')")
public class InvoiceExportController {

    private final InvoiceRepository invoiceRepository;
    private final CompanyRepository companyRepository;
    private final ClientRepository clientRepository;
    private final InvoiceZipService invoiceZipService;
    private final ExcelExportService excelExportService;
    private final JpaInvoiceRepository jpaInvoiceRepository;

    /**
     * GET /api/invoices/quarters - Get invoice counts grouped by quarter.
     * Returns summary data for the quarter selector in the frontend.
     */
    @GetMapping("/quarters")
    public ResponseEntity<Map<String, Object>> getQuarterSummary(
            @RequestParam(defaultValue = "0") int year) {

        Long companyId = CompanyContext.getCompanyId();
        if (companyId == null) {
            log.warn("No company context found for quarter summary");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Company context required"));
        }

        // Default to current year if not specified
        int targetYear = year > 0 ? year : LocalDate.now().getYear();

        log.info("GET /api/invoices/quarters - companyId: {}, year: {}", companyId, targetYear);

        try {
            // Get invoice counts grouped by quarter
            List<Object[]> quarterCounts = jpaInvoiceRepository.countByCompanyIdGroupedByQuarter(companyId, targetYear);

            // Build response with all quarters (default to 0 count)
            Map<String, Long> quarters = new LinkedHashMap<>();
            quarters.put("Q1", 0L);
            quarters.put("Q2", 0L);
            quarters.put("Q3", 0L);
            quarters.put("Q4", 0L);

            long totalCount = 0;
            for (Object[] row : quarterCounts) {
                Integer quarter = ((Number) row[0]).intValue();
                Long count = ((Number) row[1]).longValue();
                quarters.put("Q" + quarter, count);
                totalCount += count;
            }

            // Determine current quarter
            int currentQuarter = (LocalDate.now().getMonthValue() - 1) / 3 + 1;

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("year", targetYear);
            response.put("currentQuarter", currentQuarter);
            response.put("quarters", quarters);
            response.put("totalCount", totalCount);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting quarter summary: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error retrieving quarter summary"));
        }
    }

    /**
     * GET /api/invoices/download-quarter - Download ZIP of all invoices in a
     * quarter.
     */
    @GetMapping("/download-quarter")
    public ResponseEntity<byte[]> downloadQuarter(
            @RequestParam int year,
            @RequestParam int quarter) {

        Long companyId = CompanyContext.getCompanyId();
        if (companyId == null) {
            log.warn("No company context found for quarter download");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (quarter < 1 || quarter > 4) {
            log.warn("Invalid quarter: {}", quarter);
            return ResponseEntity.badRequest().build();
        }

        log.info("GET /api/invoices/download-quarter - companyId: {}, year: {}, quarter: {}",
                companyId, year, quarter);

        try {
            // Get company
            Company company = companyRepository.findById(companyId)
                    .orElseThrow(() -> new RuntimeException("Company not found: " + companyId));

            // Get invoices for the quarter
            List<Invoice> invoices = invoiceRepository.findByCompanyIdAndQuarter(companyId, year, quarter);

            if (invoices.isEmpty()) {
                log.warn("No invoices found for Q{} {}", quarter, year);
                return ResponseEntity.noContent().build();
            }

            // Generate ZIP
            byte[] zipBytes = invoiceZipService.generateQuarterZip(invoices, company, year, quarter);

            String filename = String.format("Facturas_Q%d_%d.zip", quarter, year);

            log.info("Generated ZIP for Q{} {} with {} invoices, size: {} bytes",
                    quarter, year, invoices.size(), zipBytes.length);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(zipBytes);

        } catch (IOException e) {
            log.error("Error generating quarter ZIP: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/invoices/download-all - Download ZIP of all invoices in a year.
     */
    @GetMapping("/download-all")
    public ResponseEntity<byte[]> downloadAll(@RequestParam int year) {

        Long companyId = CompanyContext.getCompanyId();
        if (companyId == null) {
            log.warn("No company context found for year download");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        log.info("GET /api/invoices/download-all - companyId: {}, year: {}", companyId, year);

        try {
            // Get company
            Company company = companyRepository.findById(companyId)
                    .orElseThrow(() -> new RuntimeException("Company not found: " + companyId));

            // Get all invoices for the year
            List<Invoice> invoices = invoiceRepository.findByCompanyIdAndYear(companyId, year);

            if (invoices.isEmpty()) {
                log.warn("No invoices found for year {}", year);
                return ResponseEntity.noContent().build();
            }

            // Generate ZIP
            byte[] zipBytes = invoiceZipService.generateYearZip(invoices, company, year);

            String filename = String.format("Facturas_%d.zip", year);

            log.info("Generated ZIP for year {} with {} invoices, size: {} bytes",
                    year, invoices.size(), zipBytes.length);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(zipBytes);

        } catch (IOException e) {
            log.error("Error generating year ZIP: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/invoices/export - Export invoices to Excel format.
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportToExcel(
            @RequestParam(defaultValue = "xlsx") String format,
            @RequestParam int year,
            @RequestParam(required = false) Integer quarter) {

        Long companyId = CompanyContext.getCompanyId();
        if (companyId == null) {
            log.warn("No company context found for export");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Only XLSX format supported for now
        if (!"xlsx".equalsIgnoreCase(format)) {
            log.warn("Unsupported export format: {}", format);
            return ResponseEntity.badRequest().build();
        }

        log.info("GET /api/invoices/export - companyId: {}, year: {}, quarter: {}, format: {}",
                companyId, year, quarter, format);

        try {
            // Get company
            Company company = companyRepository.findById(companyId)
                    .orElseThrow(() -> new RuntimeException("Company not found: " + companyId));

            // Get invoices based on quarter filter
            List<Invoice> invoices;
            String filename;

            if (quarter != null && quarter >= 1 && quarter <= 4) {
                invoices = invoiceRepository.findByCompanyIdAndQuarter(companyId, year, quarter);
                filename = String.format("Facturas_Q%d_%d.xlsx", quarter, year);
            } else {
                invoices = invoiceRepository.findByCompanyIdAndYear(companyId, year);
                filename = String.format("Facturas_%d.xlsx", year);
            }

            if (invoices.isEmpty()) {
                log.warn("No invoices found for export");
                return ResponseEntity.noContent().build();
            }

            // Build client map for the export
            Map<Long, Client> clientsMap = new HashMap<>();
            for (Invoice invoice : invoices) {
                if (!clientsMap.containsKey(invoice.getClientId())) {
                    clientRepository.findById(invoice.getClientId())
                            .ifPresent(client -> clientsMap.put(invoice.getClientId(), client));
                }
            }

            // Generate Excel
            byte[] excelBytes = excelExportService.generateInvoicesExcel(invoices, company, clientsMap);

            log.info("Generated Excel for {} invoices, size: {} bytes", invoices.size(), excelBytes.length);

            return ResponseEntity.ok()
                    .contentType(MediaType
                            .parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(excelBytes);

        } catch (IOException e) {
            log.error("Error generating Excel export: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
