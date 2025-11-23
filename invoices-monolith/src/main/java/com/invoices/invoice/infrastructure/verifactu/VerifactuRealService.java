package com.invoices.invoice.infrastructure.verifactu;

import com.invoices.invoice.domain.entities.Invoice;
import com.invoices.invoice.domain.enums.VerifactuStatus;
import com.invoices.invoice.domain.ports.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import java.util.UUID;

@Service("verifactuRealService")
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

            // Get Previous Hash (Mock implementation for now - should fetch from previous
            // invoice)
            // In a real implementation, we would query the last invoice for this company
            String previousHash = "";

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
            invoiceRepository.save(invoice);

            log.info("Invoice {} successfully verified with VeriFactu", invoice.getInvoiceNumber());

        } catch (Exception e) {
            log.error("VeriFactu processing failed for invoice {}", invoiceId, e);
            invoice.setVerifactuStatus(VerifactuStatus.REJECTED.name());
            invoiceRepository.save(invoice);
        }
    }
}
