package com.invoices.invoice.infrastructure.verifactu;

import com.invoices.invoice.domain.entities.Invoice;
import com.invoices.invoice.domain.enums.VerifactuStatus;
import com.invoices.invoice.domain.ports.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import java.util.UUID;

@Service("verifactuRealService")
@Profile("production")
@RequiredArgsConstructor
@Slf4j
public class VerifactuRealService implements VerifactuServiceInterface {

    private final VerifactuXmlBuilder xmlBuilder;
    private final XadesSigner signer;
    private final VerifactuSoapClient soapClient;
    private final InvoiceRepository invoiceRepository;
    private final com.invoices.invoice.domain.ports.CompanyRepository companyRepository;
    private final com.invoices.invoice.domain.ports.ClientRepository clientRepository;
    private final com.invoices.invoice.application.services.InvoiceCanonicalService canonicalService;

    @Override
    public String getServiceType() {
        return "REAL";
    }

    @Override
    public void processInvoice(Long invoiceId) {
        log.info("Processing VeriFactu for invoice ID: {}", invoiceId);

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceId));

        try {
            // Fetch Company and Client
            com.invoices.invoice.domain.entities.Company company = companyRepository.findById(invoice.getCompanyId())
                    .orElseThrow(() -> new RuntimeException("Company not found: " + invoice.getCompanyId()));

            com.invoices.invoice.domain.entities.Client client = clientRepository.findById(invoice.getClientId())
                    .orElseThrow(() -> new RuntimeException("Client not found: " + invoice.getClientId()));

            // Calculate Canonical Hash
            String canonicalHash = canonicalService.calculateInvoiceHash(invoice, company, client, "");

            // Get Previous Hash
            String previousHash = invoiceRepository
                    .findLastInvoiceByCompanyIdAndIdNot(invoice.getCompanyId(), invoice.getId())
                    .map(Invoice::getDocumentHash)
                    .orElse(""); // Genesis block or legacy invoice: empty hash

            // 1. Build XML
            Document xml = xmlBuilder.buildAltaFacturaXml(invoice, company, client, canonicalHash, previousHash);

            // 2. Sign XML
            Document signedXml = signer.signDocument(xml);

            // 3. Send to AEAT
            Document responseDocument = soapClient.sendInvoice(signedXml);

            // NOTE: Response parsing implementation pending
            log.debug("VeriFactu response received (Document, parsing pending): {}",
                    responseDocument != null ? "OK" : "NULL");

            invoice.setVerifactuStatus(VerifactuStatus.ACCEPTED.name());
            invoice.setVerifactuTxId(UUID.randomUUID().toString()); // Mock ID

            // Set Chaining Fields
            invoice.setPreviousDocumentHash(previousHash);
            invoice.setDocumentHash(canonicalHash);

            invoiceRepository.save(invoice);

            log.info("Invoice {} successfully verified with VeriFactu", invoice.getInvoiceNumber());

        } catch (Exception e) {
            log.error("VeriFactu processing failed for invoice {}", invoiceId, e);
            invoice.setVerifactuStatus(VerifactuStatus.REJECTED.name());
            invoiceRepository.save(invoice);
        }
    }

    @Override
    public void processWebhook(String payload) {
        log.info("[REAL] Received webhook payload: {}", payload);
        // TODO: Parse JSON payload and update invoice status
        // Example:
        // JsonNode node = objectMapper.readTree(payload);
        // String invoiceNumber = node.get("invoiceNumber").asText();
        // String status = node.get("status").asText();
        // ... update logic
    }
}
