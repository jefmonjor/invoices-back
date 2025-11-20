package com.invoices.invoice.infrastructure.external.jasper;

import com.invoices.invoice.domain.entities.Invoice;
import com.invoices.invoice.domain.ports.PdfGeneratorService;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JasperReports implementation of PdfGeneratorService.
 * Infrastructure adapter - domain doesn't know about JasperReports.
 */
@Service
@Slf4j
public class JasperPdfGeneratorService implements PdfGeneratorService {

    private static final String INVOICE_TEMPLATE_PATH = "/jasper-templates/invoice-template.jrxml";
    private static final String ITEMS_SUBREPORT_PATH = "/jasper-templates/invoice-items-subreport.jrxml";

    @Override
    public byte[] generatePdf(Invoice invoice) {
        try {
            log.info("Starting PDF generation for invoice ID: {}", invoice.getId());

            // Load and compile main template
            log.debug("Loading main template from: {}", INVOICE_TEMPLATE_PATH);
            InputStream templateStream = getClass().getResourceAsStream(INVOICE_TEMPLATE_PATH);
            if (templateStream == null) {
                log.error("Main template not found at path: {}. Classpath: {}",
                         INVOICE_TEMPLATE_PATH, System.getProperty("java.class.path"));
                throw new RuntimeException("Template not found: " + INVOICE_TEMPLATE_PATH);
            }
            log.debug("Main template loaded successfully, compiling...");
            JasperReport jasperReport = JasperCompileManager.compileReport(templateStream);
            log.debug("Main template compiled successfully");

            // Load and compile subreport from classpath
            log.debug("Loading subreport from: {}", ITEMS_SUBREPORT_PATH);
            InputStream subreportStream = getClass().getResourceAsStream(ITEMS_SUBREPORT_PATH);
            if (subreportStream == null) {
                log.error("Subreport not found at path: {}", ITEMS_SUBREPORT_PATH);
                throw new RuntimeException("Subreport not found: " + ITEMS_SUBREPORT_PATH);
            }
            log.debug("Subreport loaded successfully, compiling...");
            JasperReport itemsSubreport = JasperCompileManager.compileReport(subreportStream);
            log.debug("Subreport compiled successfully");

            // Build parameters and add compiled subreport
            Map<String, Object> parameters = buildParameters(invoice);
            parameters.put("SUBREPORT_ITEMS", itemsSubreport);

            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(
                List.of(invoice)
            );

            log.debug("Filling report with data...");
            JasperPrint jasperPrint = JasperFillManager.fillReport(
                jasperReport,
                parameters,
                dataSource
            );

            log.debug("Exporting report to PDF...");
            byte[] pdfBytes = JasperExportManager.exportReportToPdf(jasperPrint);
            log.info("PDF generated successfully for invoice ID: {}, size: {} bytes",
                    invoice.getId(), pdfBytes.length);
            return pdfBytes;

        } catch (JRException e) {
            log.error("JasperReports error generating PDF for invoice: {}. Error: {}",
                     invoice.getId(), e.getMessage(), e);
            throw new RuntimeException("Error generating PDF for invoice: " + invoice.getId(), e);
        } catch (Exception e) {
            log.error("Unexpected error generating PDF for invoice: {}",
                     invoice.getId(), e);
            throw new RuntimeException("Error generating PDF for invoice: " + invoice.getId(), e);
        }
    }

    @Override
    public byte[] generateCustomPdf(
        String invoiceNumber,
        Double baseAmount,
        Double irpfPercentage,
        Double rePercentage,
        Double totalAmount,
        String color,
        String textStyle
    ) {
        try {
            InputStream templateStream = getClass().getResourceAsStream(INVOICE_TEMPLATE_PATH);
            if (templateStream == null) {
                throw new RuntimeException("Template not found: " + INVOICE_TEMPLATE_PATH);
            }

            JasperReport jasperReport = JasperCompileManager.compileReport(templateStream);

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("invoiceNumber", invoiceNumber);
            parameters.put("baseAmount", baseAmount);
            parameters.put("irpfPercentage", irpfPercentage);
            parameters.put("rePercentage", rePercentage);
            parameters.put("totalAmount", totalAmount);
            parameters.put("color", color);
            parameters.put("textStyle", textStyle);

            JasperPrint jasperPrint = JasperFillManager.fillReport(
                jasperReport,
                parameters,
                new JREmptyDataSource()
            );

            return JasperExportManager.exportReportToPdf(jasperPrint);

        } catch (JRException e) {
            throw new RuntimeException("Error generating custom PDF", e);
        }
    }

    private Map<String, Object> buildParameters(Invoice invoice) {
        Map<String, Object> parameters = new HashMap<>();

        // Invoice data
        parameters.put("invoiceNumber", invoice.getInvoiceNumber());
        parameters.put("issueDate", invoice.getIssueDate());

        // Company (Emisor) data
        if (invoice.getCompany() != null) {
            parameters.put("companyName", invoice.getCompany().getBusinessName());
            parameters.put("companyTaxId", invoice.getCompany().getTaxId());
            parameters.put("companyAddress", invoice.getCompany().getFullAddress());
            parameters.put("companyPhone", invoice.getCompany().getPhone());
            parameters.put("iban", invoice.getCompany().getIban());
        } else {
            parameters.put("companyName", "");
            parameters.put("companyTaxId", "");
            parameters.put("companyAddress", "");
            parameters.put("companyPhone", "");
            parameters.put("iban", "");
        }

        // Client data
        if (invoice.getClient() != null) {
            parameters.put("clientName", invoice.getClient().getBusinessName());
            parameters.put("clientTaxId", invoice.getClient().getTaxId());
            parameters.put("clientAddress", invoice.getClient().getFullAddress());
        } else {
            parameters.put("clientName", "");
            parameters.put("clientTaxId", "");
            parameters.put("clientAddress", "");
        }

        // Amounts and totals
        parameters.put("baseAmount", invoice.calculateBaseAmount());
        parameters.put("irpfPercentage", invoice.getIrpfPercentage());
        parameters.put("irpfAmount", invoice.calculateIrpfAmount());
        parameters.put("rePercentage", invoice.getRePercentage());
        parameters.put("reAmount", invoice.calculateReAmount());

        // Calculate total VAT from items
        java.math.BigDecimal totalVat = invoice.getItems().stream()
            .map(item -> item.calculateTotal().subtract(item.calculateSubtotal()))
            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        parameters.put("vatAmount", totalVat);

        parameters.put("totalAmount", invoice.calculateTotalAmount());
        parameters.put("notes", invoice.getNotes());

        // Items as datasource
        JRBeanCollectionDataSource itemsDataSource = new JRBeanCollectionDataSource(
            invoice.getItems()
        );
        parameters.put("items", itemsDataSource);

        return parameters;
    }
}
