package com.invoices.invoice.infrastructure.services;

import com.invoices.invoice.domain.entities.Client;
import com.invoices.invoice.domain.entities.Company;
import com.invoices.invoice.domain.entities.Invoice;
import com.invoices.invoice.domain.ports.PdfGenerator;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.util.Matrix;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfGenerationServiceImpl implements PdfGenerator {

    private final TemplateEngine templateEngine;

    @Override
    public byte[] generateInvoicePdf(Invoice invoice, Company company, Client client) {
        log.info("Starting PDF generation for invoice: {}", invoice.getInvoiceNumber());

        try {
            // Phase 1: Generate PDF from HTML using OpenHTMLtoPDF
            byte[] initialPdfBytes = generatePdfFromHtml(invoice, company, client);

            // Phase 2: Post-process PDF using PDFBox (Watermark, Metadata)
            return postProcessPdf(initialPdfBytes, invoice);

        } catch (Exception e) {
            log.error("Error generating PDF for invoice: {}", invoice.getInvoiceNumber(), e);
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    private byte[] generatePdfFromHtml(Invoice invoice, Company company, Client client) throws IOException {
        Context context = new Context();
        context.setVariable("invoice", invoice);
        context.setVariable("company", company);
        context.setVariable("client", client);

        // Determine if it's a transport invoice based on items
        boolean isTransportInvoice = invoice.getItems().stream()
                .anyMatch(item -> item.getVehiclePlate() != null || item.getZone() != null
                        || item.getOrderNumber() != null);
        context.setVariable("isTransportInvoice", isTransportInvoice);

        String html = templateEngine.process("invoice/invoice-template", context);

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, "");
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        }
    }

    private byte[] postProcessPdf(byte[] pdfBytes, Invoice invoice) throws IOException {
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfBytes));
                ByteArrayOutputStream os = new ByteArrayOutputStream()) {

            // Add Metadata
            document.getDocumentInformation().setTitle("Factura " + invoice.getInvoiceNumber());
            document.getDocumentInformation().setAuthor("Transolido SL");
            document.getDocumentInformation().setCreator("Invoices System");
            document.getDocumentInformation().setKeywords("Factura, Invoice, " + invoice.getInvoiceNumber());

            // Add Watermark if not PAID
            if (!"PAID".equalsIgnoreCase(invoice.getStatus().name())) {
                addWatermark(document, "PENDIENTE");
            }

            document.save(os);
            return os.toByteArray();
        }
    }

    private void addWatermark(PDDocument document, String text) throws IOException {
        for (PDPage page : document.getPages()) {
            try (PDPageContentStream cs = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND,
                    true, true)) {
                float fontHeight = 50; // font size
                float width = page.getMediaBox().getWidth();
                float height = page.getMediaBox().getHeight();

                cs.setFont(PDType1Font.HELVETICA_BOLD, fontHeight);
                cs.setNonStrokingColor(200 / 255f, 200 / 255f, 200 / 255f); // Light gray

                // Simple approximation for centering
                float stringWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(text) / 1000 * fontHeight;
                // Rotate 45 degrees around the center
                cs.transform(Matrix.getRotateInstance(Math.toRadians(45), width / 2, height / 2));

                // Draw text centered (after rotation, coordinates are relative to center)
                cs.beginText();
                // Adjust position for rotation
                cs.newLineAtOffset(-stringWidth / 2, 0);
                cs.showText(text);
                cs.endText();
            }
        }
    }
}
