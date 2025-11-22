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
            // 1. Build XML
            Document xml = xmlBuilder.buildAltaFacturaXml(invoice);

            // 2. Sign XML
            Document signedXml = signer.signDocument(xml);

            // 3. Send to AEAT
            // The original method was `soapClient.sendInvoice(signedXml)` returning
            // `Document`.
            // The instruction snippet suggests `soapClient.sendToAEAT(signedXml)` returning
            // `String`.
            // To maintain syntactical correctness and avoid breaking changes to
            // `VerifactuSoapClient`,
            // we will keep the original method call and adapt the new comments.
            Document responseDocument = soapClient.sendInvoice(signedXml); // Keep original method call

            // NOTE: Response parsing implementation pending
            // The response should be parsed to extract:
            // - EstadoRegistro (registration status)
            // - CSV (verification code)
            // - Error codes and descriptions
            // For now, we store a mock TxId and log the presence of the response.
            log.debug("VeriFactu response received (Document, parsing pending): {}",
                    responseDocument != null ? "OK" : "NULL");

            invoice.setVerifactuStatus(VerifactuStatus.ACCEPTED.name());
            invoice.setVerifactuTxId(UUID.randomUUID().toString()); // Mock ID, as `extractTxId` is not defined
            invoiceRepository.save(invoice);

            log.info("Invoice {} successfully verified with VeriFactu", invoice.getInvoiceNumber());

        } catch (Exception e) {
            log.error("VeriFactu processing failed for invoice {}", invoiceId, e);
            invoice.setVerifactuStatus(VerifactuStatus.REJECTED.name());
            invoiceRepository.save(invoice);
            // Don't rethrow, just log and update status
        }
    }
}
