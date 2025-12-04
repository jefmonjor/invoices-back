package com.invoices.invoice.infrastructure.services;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
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
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.util.Matrix;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.awt.image.BufferedImage;
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

    @Override
    public byte[] generateInvoicePdfWithQr(Invoice invoice, Company company, Client client, String qrPayload) {
        log.info("Starting PDF generation with QR for invoice: {}", invoice.getInvoiceNumber());

        BufferedImage qrImage = null;
        try {
            // Phase 1: Generate base PDF
            byte[] basePdf = generateInvoicePdf(invoice, company, client);

            // Phase 2: Generate QR code
            qrImage = generateQRCode(qrPayload, 200, 200); // 200x200 px

            // Phase 3: Add QR code to PDF
            return addQrCodeToPdf(basePdf, qrImage, invoice);

        } catch (Exception e) {
            log.error("Error generating PDF with QR for invoice: {}", invoice.getInvoiceNumber(), e);
            throw new RuntimeException("Failed to generate PDF with QR", e);
        } finally {
            // Explicitly dispose BufferedImage to release native memory
            if (qrImage != null) {
                qrImage.flush();
            }
        }
    }

    private byte[] addQrCodeToPdf(byte[] basePdf, BufferedImage qrImage, Invoice invoice) throws IOException {
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(basePdf));
             ByteArrayOutputStream os = new ByteArrayOutputStream()) {

            PDPage firstPage = document.getPage(0);

            // Convert BufferedImage to PDImageXObject
            PDImageXObject pdImage = LosslessFactory.createFromImage(document, qrImage);

            try (PDPageContentStream contentStream = new PDPageContentStream(
                    document, firstPage, PDPageContentStream.AppendMode.APPEND, true, true)) {

                // Position QR in top-right corner (2x2 cm = ~57x57 points)
                float xPos = firstPage.getMediaBox().getWidth() - 70; // 10pt margin
                float yPos = firstPage.getMediaBox().getHeight() - 70;
                contentStream.drawImage(pdImage, xPos, yPos, 57, 57);

                // Add "VERI*FACTU: ACEPTADA" text
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 8);
                contentStream.setNonStrokingColor(0, 128 / 255f, 0); // Green
                contentStream.beginText();
                contentStream.newLineAtOffset(xPos - 40, yPos - 10);
                contentStream.showText("VERI*FACTU: ACEPTADA");
                contentStream.endText();
            }

            // Add verified watermark
            addVerifiedWatermark(document);

            document.save(os);
            return os.toByteArray();
        }
    }

    private BufferedImage generateQRCode(String data, int width, int height) throws WriterException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, width, height);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
            }
        }
        return image;
    }

    private void addVerifiedWatermark(PDDocument document) throws IOException {
        for (PDPage page : document.getPages()) {
            try (PDPageContentStream cs = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND,
                    true, true)) {
                float fontHeight = 50; // font size
                float width = page.getMediaBox().getWidth();
                float height = page.getMediaBox().getHeight();

                cs.setFont(PDType1Font.HELVETICA_BOLD, fontHeight);
                // Green color with transparency
                cs.setNonStrokingColor(0, 200 / 255f, 0); // Light green

                // Simple approximation for centering
                float stringWidth = PDType1Font.HELVETICA_BOLD.getStringWidth("VERIFICADO") / 1000 * fontHeight;
                // Rotate 45 degrees around the center
                cs.transform(Matrix.getRotateInstance(Math.toRadians(45), width / 2, height / 2));

                // Draw text centered (after rotation, coordinates are relative to center)
                cs.beginText();
                // Adjust position for rotation
                cs.newLineAtOffset(-stringWidth / 2, 0);
                cs.showText("VERIFICADO");
                cs.endText();
            }
        }
    }
}
