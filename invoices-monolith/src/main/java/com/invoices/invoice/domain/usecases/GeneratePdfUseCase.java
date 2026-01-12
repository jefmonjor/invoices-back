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

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("unused") // verifactuPublisher temporarily disabled
public class GeneratePdfUseCase {

        private final InvoiceRepository invoiceRepository;
        private final CompanyRepository companyRepository;
        private final ClientRepository clientRepository;
        private final PdfGenerator pdfGenerator;
        private final FileStorageService fileStorageService;
        private final com.invoices.invoice.domain.ports.VerifactuVerificationPublisher verifactuPublisher;
        private final com.invoices.invoice.infrastructure.services.InvoiceCanonicalizer invoiceCanonicalizer;
        private final com.invoices.document.domain.services.StorageKeyGenerator storageKeyGenerator;
        private final com.invoices.invoice.infrastructure.services.HashingService hashingService;

        @Transactional
        public Invoice execute(Long invoiceId) {
                log.info("Executing GeneratePdfUseCase for invoice ID: {}", invoiceId);

                // 1. Fetch Invoice
                Invoice invoice = invoiceRepository.findById(invoiceId)
                                .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceId));

                // 2. Fetch Company and Client
                Company company = companyRepository.findById(invoice.getCompanyId())
                                .orElseThrow(() -> new RuntimeException(
                                                "Company not found: " + invoice.getCompanyId()));

                Client client = clientRepository.findById(invoice.getClientId())
                                .orElseThrow(() -> new RuntimeException("Client not found: " + invoice.getClientId()));

                // 3. Canonicalize invoice and calculate hash (CRITICAL for VERI*FACTU)
                String canonicalJson = invoiceCanonicalizer.canonicalize(invoice, company, client);
                String canonicalHash = hashingService.calculateSha256(canonicalJson);

                // 4. Generate compact JSON (original representation)
                String compactJson = invoiceCanonicalizer.toCompactJson(invoice);

                // 5. Generate PDF
                byte[] pdfBytes = pdfGenerator.generateInvoicePdf(invoice, company, client);

                // 6. Store in MinIO
                String objectName = storageKeyGenerator.generateInvoiceKey(
                                invoice.getCompanyId(),
                                invoice.getInvoiceNumber(),
                                invoice.getIssueDate());
                FileContent fileContent = new FileContent(
                                () -> new ByteArrayInputStream(pdfBytes),
                                pdfBytes.length,
                                "application/pdf");
                fileStorageService.storeFile(objectName, fileContent);

                // 7. Update Invoice with VERI*FACTU fields
                invoice.setDocumentJson(compactJson); // Original JSON
                invoice.setCanonicalJson(canonicalJson); // Canonical JSON (for audit)
                invoice.setDocumentHash(canonicalHash); // SHA-256 of canonical JSON
                invoice.setPdfServerPath(objectName);
                // VERIFACTU DISABLED TEMPORARILY
                invoice.setVerifactuStatus("DISABLED");
                invoice.setPdfIsFinal(true); // Final PDF since no VeriFactu verification
                invoiceRepository.save(invoice);

                // 8. Enqueue for VeriFactu verification
                // VERIFACTU DISABLED - Enqueue for verification commented out
                // verifactuPublisher.enqueueForVerification(invoice.getId());
                log.info("PDF generated and stored. VeriFactu DISABLED. Invoice: {}", invoice.getInvoiceNumber());

                return invoice;
        }
}
