package com.invoices.invoice_service.infrastructure.external.jasper;

import com.invoices.invoice_service.domain.entities.Invoice;
import com.invoices.invoice_service.domain.ports.PdfGeneratorService;
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
public class JasperPdfGeneratorService implements PdfGeneratorService {

    private static final String INVOICE_TEMPLATE_PATH = "/jasper-templates/invoice-template.jrxml";

    @Override
    public byte[] generatePdf(Invoice invoice) {
        try {
            InputStream templateStream = getClass().getResourceAsStream(INVOICE_TEMPLATE_PATH);
            if (templateStream == null) {
                throw new RuntimeException("Template not found: " + INVOICE_TEMPLATE_PATH);
            }

            JasperReport jasperReport = JasperCompileManager.compileReport(templateStream);

            Map<String, Object> parameters = buildParameters(invoice);
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(
                List.of(invoice)
            );

            JasperPrint jasperPrint = JasperFillManager.fillReport(
                jasperReport,
                parameters,
                dataSource
            );

            return JasperExportManager.exportReportToPdf(jasperPrint);

        } catch (JRException e) {
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
