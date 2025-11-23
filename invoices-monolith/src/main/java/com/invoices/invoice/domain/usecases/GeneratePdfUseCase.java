package com.invoices.invoice.domain.usecases;

import com.invoices.document.domain.entities.FileContent;
import com.invoices.document.domain.ports.FileStorageService;
import com.invoices.invoice.domain.entities.Client;
import com.invoices.invoice.domain.entities.Company;
import com.invoices.invoice.domain.entities.Invoice;
import com.invoices.invoice.domain.ports.ClientRepository;
import com.invoices.invoice.domain.ports.CompanyRepository;
import com.invoices.invoice.domain.ports.InvoiceRepository;
import com.invoices.invoice.domain.ports.PdfGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeneratePdfUseCase {

    private final InvoiceRepository invoiceRepository;
    private final CompanyRepository companyRepository;
    private final ClientRepository clientRepository;
    private final PdfGenerator pdfGenerator;
    private final FileStorageService fileStorageService;
    private final com.invoices.invoice.infrastructure.messaging.RedisVerifactuProducer redisVerifactuProducer;
    private final com.invoices.invoice.infrastructure.services.InvoiceCanonicalizer invoiceCanonicalizer;

    @Transactional
    public Invoice execute(Long invoiceId) {
        log.info("Executing GeneratePdfUseCase for invoice ID: {}", invoiceId);

        // 1. Fetch Invoice
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceId));

        // 2. Fetch Company and Client
        Company company = companyRepository.findById(invoice.getCompanyId())
                .orElseThrow(() -> new RuntimeException("Company not found: " + invoice.getCompanyId()));

        Client client = clientRepository.findById(invoice.getClientId())
                .orElseThrow(() -> new RuntimeException("Client not found: " + invoice.getClientId()));

        // 3. Canonicalize invoice and calculate hash (CRITICAL for VERI*FACTU)
        String canonicalJson = invoiceCanonicalizer.canonicalize(invoice, company, client);
        String canonicalHash = calculateSha256(canonicalJson.getBytes(StandardCharsets.UTF_8));

        // 4. Generate compact JSON (original representation)
        String compactJson = invoiceCanonicalizer.toCompactJson(invoice);

        // 5. Generate PDF
        byte[] pdfBytes = pdfGenerator.generateInvoicePdf(invoice, company, client);

        // 6. Store in MinIO
        String objectName = "invoices/" + invoice.getInvoiceNumber().replace("/", "_") + ".pdf";
        FileContent fileContent = new FileContent(
                () -> new ByteArrayInputStream(pdfBytes),
                pdfBytes.length,
                "application/pdf");
        fileStorageService.storeFile(objectName, fileContent);

        // 7. Update Invoice with VERI*FACTU fields
        invoice.setDocumentJson(compactJson);           // Original JSON
        invoice.setCanonicalJson(canonicalJson);        // Canonical JSON (for audit)
        invoice.setDocumentHash(canonicalHash);         // SHA-256 of canonical JSON
        invoice.setPdfServerPath(objectName);
        invoice.setVerifactuStatus("PENDING");
        invoice.setPdfIsFinal(false);                   // Draft PDF initially
        invoiceRepository.save(invoice);

        // 8. Enqueue for VeriFactu verification
        redisVerifactuProducer.enqueueInvoice(invoice.getId());
        log.info("PDF generated, stored, and enqueued for VeriFactu. Invoice: {}, Hash: {}",
                invoice.getInvoiceNumber(), canonicalHash);

        return invoice;
    }

    private String calculateSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
}
