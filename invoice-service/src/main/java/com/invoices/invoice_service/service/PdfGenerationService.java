package com.invoices.invoice_service.service;

import com.invoices.invoice_service.dto.InvoiceDTO;
import com.invoices.invoice_service.dto.InvoiceItemDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfGenerationService {

    public byte[] generateInvoicePdf(InvoiceDTO invoiceDTO) throws Exception {
        try {
            // Cargar la plantilla JRXML
            InputStream is = new ClassPathResource("jasper/invoice.jrxml").getInputStream();
            JasperReport jasperReport = JasperCompileManager.compileReport(is);

            // Preparar los parámetros para el reporte
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("invoiceNumber", invoiceDTO.getInvoiceNumber());
            parameters.put("issueDate", invoiceDTO.getIssueDate());
            parameters.put("userId", invoiceDTO.getUserId());
            parameters.put("clientId", invoiceDTO.getClientId());

            // Datos numéricos
            parameters.put("baseAmount", invoiceDTO.getBaseAmount() != null ? invoiceDTO.getBaseAmount().toPlainString() : "0.00");
            parameters.put("vatPercentage", invoiceDTO.getVatPercentage() != null ? invoiceDTO.getVatPercentage().toPlainString() : "21.00");
            parameters.put("vatAmount", invoiceDTO.getVatAmount() != null ? invoiceDTO.getVatAmount().toPlainString() : "0.00");
            parameters.put("irpfPercentage", invoiceDTO.getIrpfPercentage() != null ? invoiceDTO.getIrpfPercentage().toPlainString() : "0.00");
            parameters.put("irpfAmount", invoiceDTO.getIrpfAmount() != null ? invoiceDTO.getIrpfAmount().toPlainString() : "0.00");
            parameters.put("rePercentage", invoiceDTO.getRePercentage() != null ? invoiceDTO.getRePercentage().toPlainString() : "0.00");
            parameters.put("reAmount", invoiceDTO.getReAmount() != null ? invoiceDTO.getReAmount().toPlainString() : "0.00");
            parameters.put("totalAmount", invoiceDTO.getTotalAmount() != null ? invoiceDTO.getTotalAmount().toPlainString() : "0.00");

            parameters.put("status", invoiceDTO.getStatus());
            parameters.put("notes", invoiceDTO.getNotes() != null ? invoiceDTO.getNotes() : "");
            parameters.put("iban", invoiceDTO.getIban() != null ? invoiceDTO.getIban() : "");

            // Datos de los ítems
            List<InvoiceItemDTO> items = invoiceDTO.getItems() != null ? invoiceDTO.getItems() : new ArrayList<>();
            JRDataSource itemsDataSource = new JRBeanCollectionDataSource(items);
            parameters.put("itemsDataSource", itemsDataSource);

            // Compilar el reporte
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, new JREmptyDataSource());

            // Exportar a PDF
            byte[] pdfBytes = JasperExportManager.exportReportToPdf(jasperPrint);

            log.info("PDF generated successfully for invoice: {}", invoiceDTO.getInvoiceNumber());
            return pdfBytes;
        } catch (Exception e) {
            log.error("Error generating PDF: ", e);
            throw new RuntimeException("Error generating invoice PDF: " + e.getMessage(), e);
        }
    }
}
