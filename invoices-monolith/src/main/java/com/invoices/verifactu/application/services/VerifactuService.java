package com.invoices.verifactu.application.services;

import com.invoices.invoice.domain.entities.Client;
import com.invoices.invoice.domain.entities.Company;
import com.invoices.invoice.domain.entities.Invoice;
import com.invoices.invoice.domain.ports.ClientRepository;
import com.invoices.invoice.domain.ports.InvoiceRepository;
import com.invoices.shared.domain.exception.BusinessException;
import com.invoices.verifactu.domain.model.AeatResponse;
import com.invoices.verifactu.domain.model.VerifactuMode;
import com.invoices.verifactu.domain.model.VerifactuResponse;
import com.invoices.verifactu.domain.ports.VerifactuPort;
import com.invoices.verifactu.infrastructure.aeat.VerifactuIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.KeyStore;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerifactuService implements VerifactuPort {

    private final InvoiceRepository invoiceRepository;
    private final ClientRepository clientRepository;
    private final InvoiceChainService chainService;
    private final CompanyCertificateService certificateService;
    private final VerifactuIntegrationService integrationService;

    @Value("${verifactu.mode:SANDBOX}")
    private String verifactuModeConfig;

    @Override
    @Transactional
    public void sendInvoice(Long invoiceId) {
        // Fetch invoice to get companyId
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new BusinessException("INVOICE_NOT_FOUND", "Invoice not found: " + invoiceId));

        sendInvoice(invoice.getCompanyId(), invoiceId);
    }

    @Override
    public void processWebhook(String payload) {
        log.info("Received Veri*Factu webhook payload: {}", payload);
        // TODO: Implement webhook processing logic (parse XML/JSON, update invoice
        // status)
    }

    @Override
    @Transactional
    public void sendInvoice(Long companyId, Long invoiceId) {
        log.info("Starting Veri*Factu send process for invoice {} of company {}", invoiceId, companyId);

        // 1. Lock company and validate invoice
        Company company = chainService.lockTenantForUpdate(companyId);
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new BusinessException("INVOICE_NOT_FOUND", "Invoice not found: " + invoiceId));

        chainService.validateInvoiceBeforeSending(invoice);

        // 2. Fetch Client (needed for XML)
        Client client = clientRepository.findById(invoice.getClientId())
                .orElseThrow(
                        () -> new BusinessException("CLIENT_NOT_FOUND", "Client not found: " + invoice.getClientId()));

        try {
            // 3. Prepare for chaining (calculate hash)
            chainService.prepareInvoiceForChaining(invoice, company);
            invoice.markAsSending();
            invoiceRepository.save(invoice);

            // 4. Get Certificate
            KeyStore certificate = certificateService.getCertificateForSigning(companyId);

            // 5. Build and Sign XML
            String xml = integrationService.buildCanonicalXML(invoice, company, client);
            String signedXml = integrationService.signXML(xml, certificate);
            invoice.setXmlContent(signedXml);

            // 6. Send to AEAT
            VerifactuMode mode = VerifactuMode.valueOf(verifactuModeConfig.toUpperCase());
            // Override mode if company has specific setting (future feature)

            AeatResponse rawResponse = integrationService.callAEATEndpoint(signedXml, mode);
            VerifactuResponse response = integrationService.parseResponse(rawResponse);

            // 7. Handle Response
            if (response.isSuccess()) {
                handleSuccess(invoice, company, response);
            } else {
                handleError(invoice, response);
            }

        } catch (Exception e) {
            log.error("Error sending invoice {} to Veri*Factu", invoiceId, e);
            invoice.markAsRejected();
            // In a real worker, we might schedule a retry here
            invoiceRepository.save(invoice);
            throw new BusinessException("VERIFACTU_SEND_ERROR", "Error sending to AEAT: " + e.getMessage());
        }
    }

    private void handleSuccess(Invoice invoice, Company company, VerifactuResponse response) {
        log.info("Invoice {} accepted by AEAT. CSV: {}", invoice.getInvoiceNumber(), response.getCsv());

        invoice.markAsSent();
        invoice.setCsvAcuse(response.getCsv());
        invoice.setQrData(response.getQrData());
        invoiceRepository.save(invoice);

        // Update company hash chain
        chainService.updateTenantLastHash(company.getId(), invoice.getHash());
    }

    private void handleError(Invoice invoice, VerifactuResponse response) {
        log.error("Invoice {} rejected by AEAT. Code: {}, Message: {}",
                invoice.getInvoiceNumber(), response.getErrorCode(), response.getErrorMessage());

        invoice.markAsRejected();
        // Store error details in invoice notes or separate audit log
        // For now, we just update status
        invoiceRepository.save(invoice);

        throw new BusinessException("VERIFACTU_REJECTED",
                "AEAT Rejected: " + response.getErrorMessage() + " (" + response.getErrorCode() + ")");
    }
}
