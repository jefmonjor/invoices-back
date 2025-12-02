package com.invoices.verifactu.infrastructure.aeat;

import com.invoices.invoice.domain.entities.Client;
import com.invoices.invoice.domain.entities.Company;
import com.invoices.invoice.domain.entities.Invoice;
import com.invoices.verifactu.domain.model.AeatResponse;
import com.invoices.verifactu.domain.model.VerifactuMode;
import com.invoices.verifactu.domain.model.VerifactuResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.KeyStore;
import java.time.format.DateTimeFormatter;

import com.invoices.shared.domain.exception.BusinessException;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.time.Duration;

import xades4j.providers.KeyingDataProvider;
import xades4j.production.XadesSigningProfile;
import xades4j.production.XadesBesSigningProfile;
import xades4j.production.XadesSigner;
import xades4j.production.SignedDataObjects;
import xades4j.properties.DataObjectDesc;
import xades4j.production.DataObjectReference;
import xades4j.algorithms.EnvelopedSignatureTransform;

/**
 * Service for integrating with AEAT (Spanish Tax Agency) Veri*Factu system.
 * Handles XML generation, digital signing, and SOAP communication.
 */
@Service
@Slf4j
public class VerifactuIntegrationService {

    @Value("${verifactu.aeat.endpoint-sandbox}")
    private String sandboxEndpoint;

    @Value("${verifactu.aeat.endpoint-production}")
    private String productionEndpoint;

    @Value("${verifactu.aeat.timeout:30000}")
    private int timeout;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final org.springframework.web.reactive.function.client.WebClient webClient;

    public VerifactuIntegrationService(
            org.springframework.web.reactive.function.client.WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

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
     * @param password    Password for the private key (usually same as keystore
     *                    password)
     * @return Signed XML string
     */
    public String signXML(String xml, KeyStore certificate, String password) {
        log.debug("Signing XML with XAdES-BES signature");

        try {
            // 1. Parse XML string to Document
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            Document document = dbf.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));

            // 2. Prepare KeyingDataProvider from KeyStore
            String alias = certificate.aliases().nextElement();
            KeyStore.PrivateKeyEntry keyEntry = (KeyStore.PrivateKeyEntry) certificate.getEntry(alias,
                    new KeyStore.PasswordProtection(password.toCharArray()));

            if (keyEntry == null) {
                throw new BusinessException("CERTIFICATE_ERROR", "No private key found in keystore",
                        org.springframework.http.HttpStatus.BAD_REQUEST);
            }

            KeyingDataProvider keyingDataProvider = new KeyingDataProvider() {
                @Override
                public List<X509Certificate> getSigningCertificateChain() {
                    try {
                        Certificate[] chain = certificate.getCertificateChain(alias);
                        List<X509Certificate> x509Chain = new ArrayList<>();
                        if (chain != null) {
                            for (Certificate c : chain) {
                                if (c instanceof X509Certificate) {
                                    x509Chain.add((X509Certificate) c);
                                }
                            }
                        }
                        return x509Chain;
                    } catch (KeyStoreException e) {
                        throw new RuntimeException("Error getting certificate chain", e);
                    }
                }

                @Override
                public PrivateKey getSigningKey(X509Certificate signingCert) {
                    try {
                        return keyEntry.getPrivateKey();
                    } catch (Exception e) {
                        throw new RuntimeException("Error getting private key", e);
                    }
                }
            };

            // 3. Create Signing Profile
            XadesSigningProfile p = new XadesBesSigningProfile(keyingDataProvider);

            // 4. Create Signer
            XadesSigner signer = p.newSigner();

            // 5. Sign
            // We sign the whole document (DataObjectDesc with empty URI means whole doc)
            DataObjectDesc obj = new DataObjectReference("");
            obj.withTransform(new EnvelopedSignatureTransform());

            signer.sign(new SignedDataObjects(obj), document.getDocumentElement());

            // 6. Convert back to String
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(writer));

            return writer.getBuffer().toString();

        } catch (Exception e) {
            log.error("Error signing XML", e);
            throw new BusinessException("SIGNATURE_ERROR", "Error signing invoice XML: " + e.getMessage(),
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        }
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

        String soapRequest = wrapInSoapEnvelope(signedXml);
        log.debug("SOAP Request: {}", soapRequest);

        try {
            // Block with timeout to prevent thread pool exhaustion
            // AEAT timeout is typically 30 seconds
            String soapResponse = webClient.post()
                    .uri(endpoint)
                    .header("Content-Type", "text/xml; charset=utf-8")
                    .bodyValue(soapRequest)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))  // Prevent indefinite blocking
                    .block(Duration.ofSeconds(35));   // Block with explicit timeout

            log.debug("SOAP Response: {}", soapResponse);
            return parseSoapResponse(soapResponse);

        } catch (java.util.concurrent.TimeoutException e) {
            log.error("Timeout calling AEAT endpoint after 30 seconds", e);
            throw new BusinessException("AEAT_TIMEOUT", "AEAT service timeout - please retry",
                    org.springframework.http.HttpStatus.GATEWAY_TIMEOUT);
        } catch (Exception e) {
            log.error("Error calling AEAT endpoint: {}", e.getMessage(), e);
            throw new BusinessException("AEAT_CONNECTION_ERROR", "Error connecting to AEAT: " + e.getMessage(),
                    org.springframework.http.HttpStatus.BAD_GATEWAY);
        }
    }

    private String wrapInSoapEnvelope(String payload) {
        // Remove XML declaration if present in payload to avoid double declaration
        String cleanPayload = payload.replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", "");

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                "<soapenv:Header/>" +
                "<soapenv:Body>" +
                cleanPayload +
                "</soapenv:Body>" +
                "</soapenv:Envelope>";
    }

    private AeatResponse parseSoapResponse(String soapResponse) {
        AeatResponse response = new AeatResponse();
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            Document doc = dbf.newDocumentBuilder().parse(new InputSource(new StringReader(soapResponse)));

            // Basic parsing logic - needs to be refined based on actual AEAT response
            // structure
            // Assuming <EstadoRegistro>Correcto</EstadoRegistro> or similar
            // This is a simplified parser for the "Unblocker" phase

            // Check for Fault
            if (doc.getElementsByTagName("soapenv:Fault").getLength() > 0) {
                response.setSuccess(false);
                response.setCode("SOAP_FAULT");
                response.setMessage(doc.getElementsByTagName("faultstring").item(0).getTextContent());
                return response;
            }

            // Look for specific Veri*Factu response tags
            // Note: tag names depend on the specific AEAT service version
            // For now, we'll look for generic success indicators or return raw content for
            // debugging

            // Placeholder logic:
            response.setSuccess(true);
            response.setCode("200");
            response.setMessage("Response received (Parsing to be implemented fully)");
            // response.setCsv(...)

            return response;
        } catch (Exception e) {
            log.error("Error parsing SOAP response", e);
            response.setSuccess(false);
            response.setCode("PARSE_ERROR");
            response.setMessage("Error parsing AEAT response: " + e.getMessage());
            return response;
        }
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
        return null;
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
