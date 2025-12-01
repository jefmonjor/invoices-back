package com.invoices.verifactu.infrastructure.aeat;

import com.invoices.invoice.domain.entities.Client;
import com.invoices.invoice.domain.entities.Company;
import com.invoices.invoice.domain.entities.Invoice;
import com.invoices.verifactu.domain.model.AeatResponse;
import com.invoices.verifactu.domain.model.VerifactuMode;
import com.invoices.verifactu.domain.model.VerifactuResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.KeyStore;
import java.time.format.DateTimeFormatter;

/**
 * Service for integrating with AEAT (Spanish Tax Agency) Veri*Factu system.
 * Handles XML generation, digital signing, and SOAP communication.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VerifactuIntegrationService {

    @Value("${verifactu.aeat.endpoint-sandbox}")
    private String sandboxEndpoint;

    @Value("${verifactu.aeat.endpoint-production}")
    private String productionEndpoint;

    @Value("${verifactu.aeat.timeout:30000}")
    private int timeout;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    /**
     * Builds canonical XML for an invoice according to Veri*Factu specifications.
     * 
     * @param invoice Invoice to convert to XML
     * @param company Issuing company
     * @param client  Recipient client
     * @return XML string ready for signing
     */
    public String buildCanonicalXML(Invoice invoice, Company company, Client client) {
        log.debug("Building canonical XML for invoice {}", invoice.getInvoiceNumber());

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xml.append(
                "<RegistroAlta xmlns=\"https://www2.agenciatributaria.gob.es/static_files/common/internet/dep/aplicaciones/es/aeat/tike/cont/ws/SuministroInformacion.xsd\">");

        // Cabecera (Header)
        xml.append("<Cabecera>");
        xml.append("<IDVersion>").append("1.0").append("</IDVersion>");
        xml.append("<Titular>");
        xml.append("<NombreRazon>").append(escapeXml(company.getBusinessName())).append("</NombreRazon>");
        xml.append("<NIF>").append(company.getTaxId()).append("</NIF>");
        xml.append("</Titular>");
        xml.append("</Cabecera>");

        // Factura (Invoice)
        xml.append("<RegistroFactura>");

        // Identificación de factura
        xml.append("<IDFactura>");
        xml.append("<IDEmisorFactura>");
        xml.append("<NIF>").append(company.getTaxId()).append("</NIF>");
        xml.append("</IDEmisorFactura>");
        xml.append("<NumSerieFactura>").append(invoice.getInvoiceNumber()).append("</NumSerieFactura>");
        xml.append("<FechaExpedicionFactura>").append(invoice.getIssueDate().format(DATE_FORMATTER))
                .append("</FechaExpedicionFactura>");
        xml.append("</IDFactura>");

        // Contraparte (Client)
        xml.append("<Contraparte>");
        xml.append("<NombreRazon>").append(escapeXml(client.getBusinessName())).append("</NombreRazon>");
        xml.append("<NIF>").append(client.getTaxId()).append("</NIF>");
        xml.append("</Contraparte>");

        // Importes (Amounts)
        xml.append("<ImporteTotal>").append(invoice.getTotalAmount()).append("</ImporteTotal>");
        xml.append("<BaseImponible>").append(invoice.getBaseAmount() != null ? invoice.getBaseAmount() : "0")
                .append("</BaseImponible>");

        // Hash encadenado (Chained hash)
        if (invoice.getHash() != null) {
            xml.append("<Huella>").append(invoice.getHash()).append("</Huella>");
        }
        if (invoice.getLastHashBefore() != null) {
            xml.append("<HuellaAnterior>").append(invoice.getLastHashBefore()).append("</HuellaAnterior>");
        }

        // Rectificativa (if applicable)
        if (invoice.getIsRectificativa() != null && invoice.getIsRectificativa()) {
            xml.append("<TipoFactura>R</TipoFactura>");
            // TODO: Add rectification details
        } else {
            xml.append("<TipoFactura>F</TipoFactura>");
        }

        xml.append("</RegistroFactura>");
        xml.append("</RegistroAlta>");

        String xmlString = xml.toString();
        log.debug("Generated XML: {} bytes", xmlString.length());
        return xmlString;
    }

    /**
     * Signs XML using XAdES-BES digital signature with provided certificate.
     * 
     * @param xml         XML string to sign
     * @param certificate Certificate from company's KeyStore
     * @return Signed XML string
     */
    public String signXML(String xml, KeyStore certificate) {
        log.debug("Signing XML with XAdES-BES signature");

        // TODO: Implement XAdES-BES signing
        // This requires:
        // 1. Load private key from certificate
        // 2. Create XML Signature (XMLSignature)
        // 3. Apply XAdES-BES transformations
        // 4. Embed signature in XML

        // For now, return a placeholder
        // In production, use libraries like:
        // - Apache Santuario (XML Security)
        // - DSS (Digital Signature Service) from EU

        log.warn("XAdES-BES signing not yet implemented - returning unsigned XML");
        return xml; // PLACEHOLDER
    }

    /**
     * Calls AEAT endpoint (sandbox or production) with signed XML.
     * 
     * @param signedXml Signed XML document
     * @param mode      Operating mode (sandbox/production)
     * @return AEAT response
     */
    public AeatResponse callAEATEndpoint(String signedXml, VerifactuMode mode) {
        String endpoint = mode == VerifactuMode.PRODUCTION ? productionEndpoint : sandboxEndpoint;
        log.info("Calling AEAT endpoint: {} (mode: {})", endpoint, mode);

        // TODO: Implement SOAP client
        // Options:
        // 1. Spring WS (WebServiceTemplate)
        // 2. Apache CXF
        // 3. JAX-WS

        // For now, simulate response
        log.warn("AEAT SOAP client not yet implemented - returning mock response");
        return createMockResponse();
    }

    /**
     * Parses AEAT SOAP response into structured object.
     * 
     * @param aeatResponse Raw SOAP response from AEAT
     * @return Parsed response
     */
    public VerifactuResponse parseResponse(AeatResponse aeatResponse) {
        log.debug("Parsing AEAT response");

        VerifactuResponse response = new VerifactuResponse();
        response.setSuccess(aeatResponse.isSuccess());
        response.setResponseCode(aeatResponse.getCode());
        response.setMessage(aeatResponse.getMessage());

        if (aeatResponse.isSuccess()) {
            response.setCsv(extractCSV(aeatResponse));
            response.setQrData(extractQRData(aeatResponse));
        } else {
            response.setErrorCode(aeatResponse.getCode());
            response.setErrorMessage(aeatResponse.getMessage());
        }

        return response;
    }

    /**
     * Extracts CSV (Código Seguro de Verificación) from AEAT response.
     */
    public String extractCSV(AeatResponse response) {
        // TODO: Parse CSV from SOAP response
        // CSV format: XXXX-XXXX-XXXX-XXXX
        if (response.getCsv() != null) {
            return response.getCsv();
        }
        log.warn("CSV extraction not fully implemented - using placeholder");
        return "CSV-" + System.currentTimeMillis(); // PLACEHOLDER
    }

    /**
     * Extracts QR data from AEAT response.
     * QR contains URL: https://aeat.es/verifactu/verify?csv=XXX
     */
    public String extractQRData(AeatResponse response) {
        String csv = extractCSV(response);
        if (csv != null) {
            return "https://www2.agenciatributaria.gob.es/wlpl/TIKE-CONT/verificar?csv=" + csv;
        }
        return null;
    }

    /**
     * Creates mock AEAT response for testing (until real SOAP client is
     * implemented).
     */
    private AeatResponse createMockResponse() {
        AeatResponse response = new AeatResponse();
        response.setSuccess(true);
        response.setCode("0000");
        response.setMessage("Factura registrada correctamente");
        response.setCsv("MOCK-CSV-" + System.currentTimeMillis());
        return response;
    }

    /**
     * Escapes XML special characters.
     */
    private String escapeXml(String value) {
        if (value == null)
            return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
