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

import org.springframework.web.client.RestClient;
import java.security.KeyStore;
import java.time.format.DateTimeFormatter;

import com.invoices.shared.domain.exception.BusinessException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
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

import xades4j.providers.KeyingDataProvider;
import xades4j.production.XadesSigningProfile;
import xades4j.production.XadesBesSigningProfile;
import xades4j.production.XadesSigner;
import xades4j.production.SignedDataObjects;
import xades4j.properties.DataObjectDesc;
import xades4j.production.DataObjectReference;
import xades4j.algorithms.EnvelopedSignatureTransform;

import com.invoices.verifactu.domain.ports.VerifactuIntegrationPort;

/**
 * Service for integrating with AEAT (Spanish Tax Agency) Veri*Factu system.
 * Handles XML generation, digital signing, and SOAP communication.
 */
@Service
@Slf4j
public class VerifactuIntegrationService implements VerifactuIntegrationPort {

    @Value("${verifactu.aeat.endpoint-sandbox}")
    private String sandboxEndpoint;

    @Value("${verifactu.aeat.endpoint-production}")
    private String productionEndpoint;

    @Value("${verifactu.aeat.timeout:30000}")
    private int timeout;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final String AEAT_VERIFICATION_URL = "https://www2.agenciatributaria.gob.es/wlpl/TIKE-CONT/verificar?csv=";

    private final RestClient restClient;

    public VerifactuIntegrationService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
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

        if (invoice.getIsRectificativa() != null && invoice.getIsRectificativa()) {
            xml.append("<TipoFactura>R</TipoFactura>");
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
            // 1. Parse XML string to Document with XXE protections
            DocumentBuilderFactory dbf = createSecureDocumentBuilderFactory();
            Document document;
            try (StringReader reader = new StringReader(xml)) {
                document = dbf.newDocumentBuilder().parse(new InputSource(reader));
            }

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

            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            try (StringWriter writer = new StringWriter()) {
                transformer.transform(new DOMSource(document), new StreamResult(writer));
                return writer.getBuffer().toString();
            }

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
            String soapResponse = restClient.post()
                    .uri(endpoint)
                    .header("Content-Type", "text/xml; charset=utf-8")
                    .body(soapRequest)
                    .retrieve()
                    .body(String.class);

            log.debug("SOAP Response: {}", soapResponse);
            return parseSoapResponse(soapResponse);

        } catch (Exception e) {
            log.error("Error calling AEAT endpoint: {}", e.getMessage(), e);
            throw new BusinessException("AEAT_CONNECTION_ERROR", "Error connecting to AEAT: " + e.getMessage(),
                    org.springframework.http.HttpStatus.BAD_GATEWAY);
        }
    }

    /**
     * Creates a DocumentBuilderFactory with XXE protections enabled.
     * Prevents XML External Entity (XXE) injection attacks.
     *
     * @return secure DocumentBuilderFactory
     * @throws Exception if security features cannot be configured
     */
    private DocumentBuilderFactory createSecureDocumentBuilderFactory() throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);

        // Disable XXE and DTD processing
        try {
            // Prevent XXE attacks by disabling DTD processing
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            dbf.setXIncludeAware(false);
            dbf.setExpandEntityReferences(false);
        } catch (Exception e) {
            log.warn("Could not disable XXE features completely, some features may not be supported: {}",
                    e.getMessage());
            // Still try to configure what we can
        }

        return dbf;
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
            // Use secure DocumentBuilderFactory with XXE protections
            DocumentBuilderFactory dbf = createSecureDocumentBuilderFactory();
            Document doc;
            try (StringReader reader = new StringReader(soapResponse)) {
                doc = dbf.newDocumentBuilder().parse(new InputSource(reader));
            }

            // Check for SOAP Fault first
            NodeList faultNodes = doc.getElementsByTagName("soapenv:Fault");
            if (faultNodes.getLength() == 0) {
                faultNodes = doc.getElementsByTagName("soap:Fault");
            }
            if (faultNodes.getLength() > 0) {
                response.setSuccess(false);
                response.setCode("SOAP_FAULT");
                NodeList faultStrings = doc.getElementsByTagName("faultstring");
                if (faultStrings.getLength() > 0) {
                    response.setMessage(faultStrings.item(0).getTextContent());
                }
                return response;
            }

            // Parse VeriFactu response - look for standard response elements
            // Namespace:
            // https://www2.agenciatributaria.gob.es/static_files/common/internet/dep/aplicaciones/es/aeat/tike/cont/ws/SusministroLR.xsd

            // Try to find CSV (Código Seguro de Verificación)
            String csv = extractElementText(doc, "CSV");
            if (csv == null) {
                csv = extractElementText(doc, "CodigoCSV");
            }

            // Try to find the status (EstadoEnvio or EstadoRegistro)
            String estado = extractElementText(doc, "EstadoEnvio");
            if (estado == null) {
                estado = extractElementText(doc, "EstadoRegistro");
            }

            // Try to find error information
            String codigoError = extractElementText(doc, "CodigoErrorRegistro");
            String descError = extractElementText(doc, "DescripcionErrorRegistro");

            // Alternative error fields
            if (codigoError == null) {
                codigoError = extractElementText(doc, "CodigoError");
            }
            if (descError == null) {
                descError = extractElementText(doc, "MensajeAdicional");
            }

            // Determine success based on estado
            boolean isSuccess = "Correcto".equalsIgnoreCase(estado) ||
                    "Aceptado".equalsIgnoreCase(estado) ||
                    "AceptadoConErrores".equalsIgnoreCase(estado);

            response.setSuccess(isSuccess);
            response.setCode(isSuccess ? "OK" : (codigoError != null ? codigoError : "ERROR"));
            response.setCsv(csv);

            if (isSuccess) {
                response.setMessage("Factura registrada correctamente en AEAT");
            } else {
                response.setMessage(descError != null ? descError : "Error en registro AEAT");
            }

            log.info("Parsed AEAT response: success={}, csv={}, estado={}", isSuccess, csv, estado);
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
     * Extracts text content from first element with given tag name.
     */
    private String extractElementText(Document doc, String tagName) {
        // Try without namespace
        NodeList nodes = doc.getElementsByTagName(tagName);
        if (nodes.getLength() > 0 && nodes.item(0).getTextContent() != null) {
            String text = nodes.item(0).getTextContent().trim();
            return text.isEmpty() ? null : text;
        }

        // Try with common namespace prefixes
        String[] prefixes = { "sii:", "vf:", "ns2:", "ns3:" };
        for (String prefix : prefixes) {
            nodes = doc.getElementsByTagName(prefix + tagName);
            if (nodes.getLength() > 0 && nodes.item(0).getTextContent() != null) {
                String text = nodes.item(0).getTextContent().trim();
                return text.isEmpty() ? null : text;
            }
        }

        return null;
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

    public String extractCSV(AeatResponse response) {
        return response.getCsv();
    }

    public String extractQRData(AeatResponse response) {
        String csv = extractCSV(response);
        return csv != null ? AEAT_VERIFICATION_URL + csv : null;
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
