package com.invoices.invoice.infrastructure.verifactu;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.core.support.WebServiceGatewaySupport;
import org.w3c.dom.Document;

@Component
@Slf4j
public class VerifactuSoapClient extends WebServiceGatewaySupport {

    public Document sendInvoice(Document signedXml) {
        log.info("Sending signed XML to AEAT VeriFactu...");

        // In a real implementation, we would marshal the Document or use a specific
        // request object
        // For this skeleton, we assume the WebServiceTemplate is configured to handle
        // DOM or raw XML

        try {
            // This is a simplified call. Real implementation might need specific SOAP
            // Action callback
            // and response unmarshalling.
            // Object response = getWebServiceTemplate().marshalSendAndReceive(signedXml);

            log.info("Mocking successful send to AEAT");
            return signedXml; // Return the request as mock response for now
        } catch (Exception e) {
            log.error("Error sending to AEAT", e);
            throw new RuntimeException("Failed to communicate with VeriFactu", e);
        }
    }
}
