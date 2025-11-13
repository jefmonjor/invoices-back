package com.invoices.invoice_service.service;

import com.invoices.invoice_service.entity.Invoice;
import com.invoices.invoice_service.entity.InvoiceItem;
import com.invoices.invoice_service.exception.PdfGenerationException;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.design.*;
import net.sf.jasperreports.engine.type.HorizontalTextAlignEnum;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio para generar PDFs de facturas usando JasperReports
 */
@Service
@Slf4j
public class PdfGenerationService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));

    /**
     * Genera un PDF para una factura
     * @param invoice Factura a convertir en PDF
     * @return Array de bytes con el PDF generado
     * @throws PdfGenerationException Si hay un error generando el PDF
     */
    public byte[] generateInvoicePdf(Invoice invoice) {
        try {
            log.info("Generando PDF para factura: {}", invoice.getInvoiceNumber());

            // Crear el diseño del reporte programáticamente
            JasperDesign jasperDesign = createInvoiceDesign();

            // Compilar el diseño
            JasperReport jasperReport = JasperCompileManager.compileReport(jasperDesign);

            // Preparar los datos
            Map<String, Object> parameters = prepareParameters(invoice);
            JRBeanCollectionDataSource itemsDataSource = new JRBeanCollectionDataSource(
                    prepareItemsData(invoice.getItems())
            );

            // Llenar el reporte
            JasperPrint jasperPrint = JasperFillManager.fillReport(
                    jasperReport,
                    parameters,
                    itemsDataSource
            );

            // Exportar a PDF
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            JasperExportManager.exportReportToPdfStream(jasperPrint, outputStream);

            byte[] pdfBytes = outputStream.toByteArray();
            log.info("PDF generado exitosamente para factura: {}. Tamaño: {} bytes",
                    invoice.getInvoiceNumber(), pdfBytes.length);

            return pdfBytes;

        } catch (JRException e) {
            log.error("Error generando PDF para factura: {}", invoice.getInvoiceNumber(), e);
            throw new PdfGenerationException("Error generando PDF de la factura", e);
        }
    }

    /**
     * Crea el diseño del reporte de factura programáticamente
     */
    private JasperDesign createInvoiceDesign() throws JRException {
        JasperDesign jasperDesign = new JasperDesign();
        jasperDesign.setName("invoice_report");
        jasperDesign.setPageWidth(595);
        jasperDesign.setPageHeight(842);
        jasperDesign.setColumnWidth(555);
        jasperDesign.setLeftMargin(20);
        jasperDesign.setRightMargin(20);
        jasperDesign.setTopMargin(30);
        jasperDesign.setBottomMargin(30);

        // Definir campos
        addField(jasperDesign, "invoiceNumber", String.class);
        addField(jasperDesign, "clientEmail", String.class);
        addField(jasperDesign, "invoiceDate", String.class);
        addField(jasperDesign, "dueDate", String.class);
        addField(jasperDesign, "status", String.class);
        addField(jasperDesign, "notes", String.class);
        addField(jasperDesign, "description", String.class);
        addField(jasperDesign, "quantity", Integer.class);
        addField(jasperDesign, "unitPrice", String.class);
        addField(jasperDesign, "total", String.class);

        // Definir parámetros
        addParameter(jasperDesign, "subtotalFormatted", String.class);
        addParameter(jasperDesign, "taxFormatted", String.class);
        addParameter(jasperDesign, "totalFormatted", String.class);

        // Crear bandas
        createTitleBand(jasperDesign);
        createColumnHeaderBand(jasperDesign);
        createDetailBand(jasperDesign);
        createSummaryBand(jasperDesign);

        return jasperDesign;
    }

    /**
     * Crea la banda de título
     */
    private void createTitleBand(JasperDesign jasperDesign) throws JRException {
        JRDesignBand titleBand = new JRDesignBand();
        titleBand.setHeight(150);

        // Título "FACTURA"
        addStaticText(titleBand, "FACTURA", 0, 10, 555, 30, 20, true);

        // Número de factura
        addStaticText(titleBand, "Número:", 0, 50, 100, 20, 12, true);
        addTextField(titleBand, "$F{invoiceNumber}", 100, 50, 200, 20, 12, false);

        // Cliente
        addStaticText(titleBand, "Cliente:", 0, 75, 100, 20, 12, true);
        addTextField(titleBand, "$F{clientEmail}", 100, 75, 400, 20, 12, false);

        // Fecha
        addStaticText(titleBand, "Fecha:", 0, 100, 100, 20, 12, true);
        addTextField(titleBand, "$F{invoiceDate}", 100, 100, 150, 20, 12, false);

        // Vencimiento
        addStaticText(titleBand, "Vencimiento:", 270, 100, 100, 20, 12, true);
        addTextField(titleBand, "$F{dueDate}", 370, 100, 150, 20, 12, false);

        // Estado
        addStaticText(titleBand, "Estado:", 0, 125, 100, 20, 12, true);
        addTextField(titleBand, "$F{status}", 100, 125, 150, 20, 12, false);

        jasperDesign.setTitle(titleBand);
    }

    /**
     * Crea la banda de encabezado de columnas
     */
    private void createColumnHeaderBand(JasperDesign jasperDesign) throws JRException {
        JRDesignBand columnHeaderBand = new JRDesignBand();
        columnHeaderBand.setHeight(30);

        addStaticText(columnHeaderBand, "Descripción", 0, 5, 250, 20, 12, true);
        addStaticText(columnHeaderBand, "Cantidad", 260, 5, 80, 20, 12, true);
        addStaticText(columnHeaderBand, "Precio Unit.", 350, 5, 100, 20, 12, true);
        addStaticText(columnHeaderBand, "Total", 460, 5, 95, 20, 12, true);

        jasperDesign.setColumnHeader(columnHeaderBand);
    }

    /**
     * Crea la banda de detalle (items)
     */
    private void createDetailBand(JasperDesign jasperDesign) throws JRException {
        JRDesignBand detailBand = new JRDesignBand();
        detailBand.setHeight(25);

        addTextField(detailBand, "$F{description}", 0, 5, 250, 15, 10, false);
        addTextField(detailBand, "$F{quantity}", 260, 5, 80, 15, 10, false);
        addTextField(detailBand, "$F{unitPrice}", 350, 5, 100, 15, 10, false);
        addTextField(detailBand, "$F{total}", 460, 5, 95, 15, 10, false);

        jasperDesign.setDetail(detailBand);
    }

    /**
     * Crea la banda de resumen (totales)
     */
    private void createSummaryBand(JasperDesign jasperDesign) throws JRException {
        JRDesignBand summaryBand = new JRDesignBand();
        summaryBand.setHeight(120);

        // Subtotal
        addStaticText(summaryBand, "Subtotal:", 350, 10, 100, 20, 12, true);
        addTextField(summaryBand, "$P{subtotalFormatted}", 460, 10, 95, 20, 12, false);

        // Tax
        addStaticText(summaryBand, "IVA (19%):", 350, 35, 100, 20, 12, true);
        addTextField(summaryBand, "$P{taxFormatted}", 460, 35, 95, 20, 12, false);

        // Total
        addStaticText(summaryBand, "TOTAL:", 350, 60, 100, 20, 14, true);
        addTextField(summaryBand, "$P{totalFormatted}", 460, 60, 95, 20, 14, true);

        // Notas
        addStaticText(summaryBand, "Notas:", 0, 90, 100, 20, 10, true);
        addTextField(summaryBand, "$F{notes} != null ? $F{notes} : \"\"", 0, 110, 555, 20, 10, false);

        jasperDesign.setSummary(summaryBand);
    }

    /**
     * Agrega un campo al diseño
     */
    private void addField(JasperDesign jasperDesign, String name, Class<?> type) throws JRException {
        JRDesignField field = new JRDesignField();
        field.setName(name);
        field.setValueClass(type);
        jasperDesign.addField(field);
    }

    /**
     * Agrega un parámetro al diseño
     */
    private void addParameter(JasperDesign jasperDesign, String name, Class<?> type) throws JRException {
        JRDesignParameter parameter = new JRDesignParameter();
        parameter.setName(name);
        parameter.setValueClass(type);
        jasperDesign.addParameter(parameter);
    }

    /**
     * Agrega un texto estático a una banda
     */
    private void addStaticText(JRDesignBand band, String text, int x, int y, int width, int height,
                                int fontSize, boolean bold) throws JRException {
        JRDesignStaticText staticText = new JRDesignStaticText();
        staticText.setX(x);
        staticText.setY(y);
        staticText.setWidth(width);
        staticText.setHeight(height);
        staticText.setText(text);

        JRDesignStyle style = new JRDesignStyle();
        style.setFontSize((float) fontSize);
        style.setBold(bold);
        staticText.setStyle(style);

        band.addElement(staticText);
    }

    /**
     * Agrega un campo de texto a una banda
     */
    private void addTextField(JRDesignBand band, String expression, int x, int y, int width, int height,
                               int fontSize, boolean bold) throws JRException {
        JRDesignTextField textField = new JRDesignTextField();
        textField.setX(x);
        textField.setY(y);
        textField.setWidth(width);
        textField.setHeight(height);

        JRDesignExpression jrExpression = new JRDesignExpression();
        jrExpression.setText(expression);
        textField.setExpression(jrExpression);

        JRDesignStyle style = new JRDesignStyle();
        style.setFontSize((float) fontSize);
        style.setBold(bold);
        textField.setStyle(style);

        band.addElement(textField);
    }

    /**
     * Prepara los parámetros para el reporte
     */
    private Map<String, Object> prepareParameters(Invoice invoice) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("subtotalFormatted", CURRENCY_FORMATTER.format(invoice.getSubtotal()));
        parameters.put("taxFormatted", CURRENCY_FORMATTER.format(invoice.getTax()));
        parameters.put("totalFormatted", CURRENCY_FORMATTER.format(invoice.getTotal()));
        return parameters;
    }

    /**
     * Prepara los datos de los items para el datasource
     */
    private List<Map<String, Object>> prepareItemsData(List<InvoiceItem> items) {
        return items.stream().map(item -> {
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("description", item.getDescription());
            itemMap.put("quantity", item.getQuantity());
            itemMap.put("unitPrice", CURRENCY_FORMATTER.format(item.getUnitPrice()));
            itemMap.put("total", CURRENCY_FORMATTER.format(item.getTotal()));
            itemMap.put("invoiceNumber", item.getInvoice() != null ? item.getInvoice().getInvoiceNumber() : "");
            itemMap.put("clientEmail", item.getInvoice() != null ? item.getInvoice().getClientEmail() : "");
            itemMap.put("invoiceDate", item.getInvoice() != null && item.getInvoice().getInvoiceDate() != null
                    ? item.getInvoice().getInvoiceDate().format(DATE_FORMATTER) : "");
            itemMap.put("dueDate", item.getInvoice() != null && item.getInvoice().getDueDate() != null
                    ? item.getInvoice().getDueDate().format(DATE_FORMATTER) : "N/A");
            itemMap.put("status", item.getInvoice() != null ? item.getInvoice().getStatus().toString() : "");
            itemMap.put("notes", item.getInvoice() != null ? item.getInvoice().getNotes() : "");
            return itemMap;
        }).collect(Collectors.toList());
    }
}
