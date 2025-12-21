package com.invoices.invoice.infrastructure.services;

import com.invoices.invoice.domain.entities.Client;
import com.invoices.invoice.domain.entities.Company;
import com.invoices.invoice.domain.entities.Invoice;
import com.invoices.invoice.domain.entities.InvoiceItem;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Service for exporting invoices to Excel format using Apache POI.
 * Generates XLSX files with proper formatting and multiple sheets.
 */
@Service
@Slf4j
public class ExcelExportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Generates an Excel file containing invoice data.
     *
     * @param invoices List of invoices to export
     * @param company  The company that owns the invoices
     * @param clients  Map of client IDs to Client entities
     * @return byte array containing the XLSX file
     * @throws IOException if Excel generation fails
     */
    public byte[] generateInvoicesExcel(List<Invoice> invoices, Company company, Map<Long, Client> clients)
            throws IOException {
        log.info("Generating Excel for {} invoices of company {}", invoices.size(), company.getId());

        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            // Create styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);
            CellStyle percentStyle = createPercentStyle(workbook);

            // Create main invoices sheet
            Sheet invoicesSheet = workbook.createSheet("Facturas");
            createInvoicesSheet(invoicesSheet, invoices, clients, headerStyle, dateStyle, currencyStyle, percentStyle);

            // Create items sheet for detailed line items
            Sheet itemsSheet = workbook.createSheet("Líneas de Factura");
            createItemsSheet(itemsSheet, invoices, headerStyle, dateStyle, currencyStyle, percentStyle);

            // Create summary sheet
            Sheet summarySheet = workbook.createSheet("Resumen");
            createSummarySheet(summarySheet, invoices, company, headerStyle, currencyStyle);

            workbook.write(baos);
            log.info("Excel generation complete for {} invoices", invoices.size());

            return baos.toByteArray();
        }
    }

    private void createInvoicesSheet(Sheet sheet, List<Invoice> invoices, Map<Long, Client> clients,
            CellStyle headerStyle, CellStyle dateStyle,
            CellStyle currencyStyle, CellStyle percentStyle) {
        // Headers
        String[] headers = {
                "Nº Factura", "Fecha", "Cliente", "CIF Cliente",
                "Base Imponible", "% IVA", "IVA", "% IRPF", "IRPF",
                "% RE", "RE", "Total", "Estado VeriFactu"
        };

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data rows
        int rowNum = 1;
        for (Invoice invoice : invoices) {
            Row row = sheet.createRow(rowNum++);
            Client client = clients.get(invoice.getClientId());

            int col = 0;

            // Invoice Number
            row.createCell(col++).setCellValue(invoice.getInvoiceNumber());

            // Issue Date
            Cell dateCell = row.createCell(col++);
            if (invoice.getIssueDate() != null) {
                dateCell.setCellValue(invoice.getIssueDate().format(DATE_FORMATTER));
            }
            dateCell.setCellStyle(dateStyle);

            // Client Name
            row.createCell(col++).setCellValue(client != null ? client.getBusinessName() : "N/A");

            // Client Tax ID
            row.createCell(col++).setCellValue(client != null ? client.getTaxId() : "N/A");

            // Base Amount
            Cell baseCell = row.createCell(col++);
            baseCell.setCellValue(invoice.getBaseAmount() != null ? invoice.getBaseAmount().doubleValue() : 0);
            baseCell.setCellStyle(currencyStyle);

            // VAT Percentage (from first item or average)
            Cell vatPctCell = row.createCell(col++);
            BigDecimal avgVat = calculateAverageVat(invoice.getItems());
            vatPctCell.setCellValue(avgVat.doubleValue());
            vatPctCell.setCellStyle(percentStyle);

            // VAT Amount (calculated from base * vat%)
            Cell vatCell = row.createCell(col++);
            BigDecimal vatAmount = calculateVatAmount(invoice);
            vatCell.setCellValue(vatAmount.doubleValue());
            vatCell.setCellStyle(currencyStyle);

            // IRPF Percentage
            Cell irpfPctCell = row.createCell(col++);
            irpfPctCell
                    .setCellValue(invoice.getIrpfPercentage() != null ? invoice.getIrpfPercentage().doubleValue() : 0);
            irpfPctCell.setCellStyle(percentStyle);

            // IRPF Amount
            Cell irpfCell = row.createCell(col++);
            BigDecimal irpfAmount = invoice.calculateIrpfAmount();
            irpfCell.setCellValue(irpfAmount != null ? irpfAmount.doubleValue() : 0);
            irpfCell.setCellStyle(currencyStyle);

            // RE Percentage
            Cell rePctCell = row.createCell(col++);
            rePctCell.setCellValue(invoice.getRePercentage() != null ? invoice.getRePercentage().doubleValue() : 0);
            rePctCell.setCellStyle(percentStyle);

            // RE Amount
            Cell reCell = row.createCell(col++);
            BigDecimal reAmount = invoice.calculateReAmount();
            reCell.setCellValue(reAmount != null ? reAmount.doubleValue() : 0);
            reCell.setCellStyle(currencyStyle);

            // Total Amount
            Cell totalCell = row.createCell(col++);
            totalCell.setCellValue(invoice.getTotalAmount() != null ? invoice.getTotalAmount().doubleValue() : 0);
            totalCell.setCellStyle(currencyStyle);

            // VeriFactu Status
            row.createCell(col)
                    .setCellValue(invoice.getVerifactuStatus() != null ? invoice.getVerifactuStatus() : "N/A");
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // Freeze header row
        sheet.createFreezePane(0, 1);
    }

    private void createItemsSheet(Sheet sheet, List<Invoice> invoices,
            CellStyle headerStyle, CellStyle dateStyle,
            CellStyle currencyStyle, CellStyle percentStyle) {
        // Headers for line items (including transport-specific fields)
        String[] headers = {
                "Nº Factura", "Descripción", "Fecha", "Matrícula", "Pedido",
                "Zona", "Unidades", "Precio", "% IVA", "% Descuento",
                "% Gas", "Subtotal", "Total"
        };

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data rows
        int rowNum = 1;
        for (Invoice invoice : invoices) {
            for (InvoiceItem item : invoice.getItems()) {
                Row row = sheet.createRow(rowNum++);
                int col = 0;

                row.createCell(col++).setCellValue(invoice.getInvoiceNumber());
                row.createCell(col++).setCellValue(item.getDescription());

                // Item Date (for transport invoices)
                Cell itemDateCell = row.createCell(col++);
                if (item.getItemDate() != null) {
                    itemDateCell.setCellValue(item.getItemDate().format(DATE_FORMATTER));
                }
                itemDateCell.setCellStyle(dateStyle);

                // Vehicle Plate
                row.createCell(col++).setCellValue(item.getVehiclePlate() != null ? item.getVehiclePlate() : "");

                // Order Number
                row.createCell(col++).setCellValue(item.getOrderNumber() != null ? item.getOrderNumber() : "");

                // Zone
                row.createCell(col++).setCellValue(item.getZone() != null ? item.getZone() : "");

                // Units
                row.createCell(col++).setCellValue(item.getUnits());

                // Price
                Cell priceCell = row.createCell(col++);
                priceCell.setCellValue(item.getPrice() != null ? item.getPrice().doubleValue() : 0);
                priceCell.setCellStyle(currencyStyle);

                // VAT Percentage
                Cell vatPctCell = row.createCell(col++);
                vatPctCell.setCellValue(item.getVatPercentage() != null ? item.getVatPercentage().doubleValue() : 0);
                vatPctCell.setCellStyle(percentStyle);

                // Discount Percentage
                Cell discountCell = row.createCell(col++);
                discountCell.setCellValue(
                        item.getDiscountPercentage() != null ? item.getDiscountPercentage().doubleValue() : 0);
                discountCell.setCellStyle(percentStyle);

                // Gas Percentage
                Cell gasCell = row.createCell(col++);
                gasCell.setCellValue(item.getGasPercentage() != null ? item.getGasPercentage().doubleValue() : 0);
                gasCell.setCellStyle(percentStyle);

                // Subtotal
                Cell subtotalCell = row.createCell(col++);
                subtotalCell.setCellValue(item.getSubtotal() != null ? item.getSubtotal().doubleValue() : 0);
                subtotalCell.setCellStyle(currencyStyle);

                // Total
                Cell totalCell = row.createCell(col);
                totalCell.setCellValue(item.getTotal() != null ? item.getTotal().doubleValue() : 0);
                totalCell.setCellStyle(currencyStyle);
            }
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // Freeze header row
        sheet.createFreezePane(0, 1);
    }

    private void createSummarySheet(Sheet sheet, List<Invoice> invoices, Company company,
            CellStyle headerStyle, CellStyle currencyStyle) {
        int rowNum = 0;

        // Company info
        Row companyRow = sheet.createRow(rowNum++);
        companyRow.createCell(0).setCellValue("Empresa:");
        companyRow.createCell(1).setCellValue(company.getBusinessName());

        Row cifRow = sheet.createRow(rowNum++);
        cifRow.createCell(0).setCellValue("CIF:");
        cifRow.createCell(1).setCellValue(company.getTaxId());

        rowNum++; // Empty row

        // Summary statistics
        Row countRow = sheet.createRow(rowNum++);
        countRow.createCell(0).setCellValue("Total Facturas:");
        countRow.createCell(1).setCellValue(invoices.size());

        // Calculate totals
        BigDecimal totalBase = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        long acceptedCount = 0;
        long pendingCount = 0;
        long rejectedCount = 0;

        for (Invoice invoice : invoices) {
            if (invoice.getBaseAmount() != null) {
                totalBase = totalBase.add(invoice.getBaseAmount());
            }
            if (invoice.getTotalAmount() != null) {
                totalAmount = totalAmount.add(invoice.getTotalAmount());
            }

            String status = invoice.getVerifactuStatus();
            if ("ACCEPTED".equals(status))
                acceptedCount++;
            else if ("PENDING".equals(status) || "PROCESSING".equals(status))
                pendingCount++;
            else if ("REJECTED".equals(status) || "FAILED".equals(status))
                rejectedCount++;
        }

        Row baseRow = sheet.createRow(rowNum++);
        baseRow.createCell(0).setCellValue("Total Base Imponible:");
        Cell baseTotalCell = baseRow.createCell(1);
        baseTotalCell.setCellValue(totalBase.doubleValue());
        baseTotalCell.setCellStyle(currencyStyle);

        Row totalRow = sheet.createRow(rowNum++);
        totalRow.createCell(0).setCellValue("Total Facturado:");
        Cell totalCell = totalRow.createCell(1);
        totalCell.setCellValue(totalAmount.doubleValue());
        totalCell.setCellStyle(currencyStyle);

        rowNum++; // Empty row

        // VeriFactu status breakdown
        Row statusHeader = sheet.createRow(rowNum++);
        statusHeader.createCell(0).setCellValue("Estado VeriFactu");
        statusHeader.getCell(0).setCellStyle(headerStyle);
        statusHeader.createCell(1).setCellValue("Cantidad");
        statusHeader.getCell(1).setCellStyle(headerStyle);

        Row acceptedRow = sheet.createRow(rowNum++);
        acceptedRow.createCell(0).setCellValue("Aceptadas:");
        acceptedRow.createCell(1).setCellValue(acceptedCount);

        Row pendingRow = sheet.createRow(rowNum++);
        pendingRow.createCell(0).setCellValue("Pendientes:");
        pendingRow.createCell(1).setCellValue(pendingCount);

        Row rejectedRow = sheet.createRow(rowNum);
        rejectedRow.createCell(0).setCellValue("Rechazadas:");
        rejectedRow.createCell(1).setCellValue(rejectedCount);

        // Auto-size columns
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0.00 €"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }

    private CellStyle createPercentStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("0.00%"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }

    private BigDecimal calculateAverageVat(List<InvoiceItem> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        // Return the VAT of the first item (most common case is all items have same
        // VAT)
        return items.get(0).getVatPercentage() != null ? items.get(0).getVatPercentage() : BigDecimal.ZERO;
    }

    private BigDecimal calculateVatAmount(Invoice invoice) {
        if (invoice.getBaseAmount() == null || invoice.getItems() == null || invoice.getItems().isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal avgVat = calculateAverageVat(invoice.getItems());
        return invoice.getBaseAmount().multiply(avgVat).divide(BigDecimal.valueOf(100));
    }
}
