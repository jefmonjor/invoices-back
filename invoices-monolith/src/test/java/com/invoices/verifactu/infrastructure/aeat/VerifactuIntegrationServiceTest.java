package com.invoices.verifactu.infrastructure.aeat;

import com.invoices.invoice.domain.entities.Client;
import com.invoices.invoice.domain.entities.Company;
import com.invoices.invoice.domain.entities.Invoice;
import com.invoices.verifactu.domain.model.AeatResponse;
import com.invoices.verifactu.domain.model.VerifactuResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.security.KeyStore;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class VerifactuIntegrationServiceTest {

    @Mock
    private org.springframework.web.client.RestClient.Builder restClientBuilder;

    @Mock
    private org.springframework.web.client.RestClient restClient;

    private VerifactuIntegrationService service;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(restClientBuilder.build()).thenReturn(restClient);
        service = new VerifactuIntegrationService(restClientBuilder);
        ReflectionTestUtils.setField(service, "sandboxEndpoint", "https://sandbox.aeat.es");
        ReflectionTestUtils.setField(service, "productionEndpoint", "https://aeat.es");
    }

    @Test
    void buildCanonicalXML_ShouldGenerateValidXmlStructure() {
        // Arrange
        Company company = new Company(
                1L, "Test Company", "B12345678", "Address", "City", "28001", "Madrid", "600000000",
                "email@test.com", "ES0000000000000000000000");

        Client client = new Client(
                1L, "Test Client", "12345678Z", "Address", "City", "28001", "Madrid", "Spain", "600000000",
                "email@client.com", 1L);

        Invoice invoice = new Invoice(
                1L, 1L, 1L, "INV-001", LocalDateTime.now(), BigDecimal.ZERO, BigDecimal.ZERO);
        // Add item to generate amount
        // We need an InvoiceItem constructor. Assuming one exists or we can mock it.
        // Since InvoiceItem is not visible here, let's check if we can add it.
        // Actually, buildCanonicalXML might rely on getBaseAmount().
        // Let's rely on the fact that Invoice initializes with 0 amounts if no items.

        // Act
        String xml = service.buildCanonicalXML(invoice, company, client);

        // Assert
        assertNotNull(xml);
        assertTrue(xml.contains("<RegistroAlta"));
        assertTrue(xml.contains("<IDVersion>1.0</IDVersion>"));
        assertTrue(xml.contains("<NumSerieFactura>INV-001</NumSerieFactura>"));
        assertTrue(xml.contains("<NIF>B12345678</NIF>"));
    }

    @Test
    void parseResponse_ShouldParseSuccessResponse() {
        // Arrange
        AeatResponse aeatResponse = new AeatResponse();
        aeatResponse.setSuccess(true);
        aeatResponse.setCode("0000");
        aeatResponse.setMessage("Success");
        aeatResponse.setCsv("CSV-123");

        // Act
        VerifactuResponse response = service.parseResponse(aeatResponse);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("CSV-123", response.getCsv());
        assertNotNull(response.getQrData());
    }

    @Test
    void parseResponse_ShouldParseErrorResponse() {
        // Arrange
        AeatResponse aeatResponse = new AeatResponse();
        aeatResponse.setSuccess(false);
        aeatResponse.setCode("ERR-01");
        aeatResponse.setMessage("Invalid format");

        // Act
        VerifactuResponse response = service.parseResponse(aeatResponse);

        // Assert
        assertFalse(response.isSuccess());
        assertEquals("ERR-01", response.getErrorCode());
        assertEquals("Invalid format", response.getErrorMessage());
    }

    @Test
    void signXML_ShouldGenerateSignedXml() throws Exception {
        // Arrange
        String xml = "<root><data>test</data></root>";
        KeyStore keyStore = generateTestKeyStore();

        // Act
        String signedXml = service.signXML(xml, keyStore, "");

        // Assert
        assertNotNull(signedXml);
        assertTrue(signedXml.contains("ds:Signature"));
        assertTrue(signedXml.contains("xades:QualifyingProperties"));
    }

    private KeyStore generateTestKeyStore() throws Exception {
        // Generate KeyPair
        java.security.KeyPairGenerator keyGen = java.security.KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        java.security.KeyPair keyPair = keyGen.generateKeyPair();

        // Generate Self-Signed Certificate using Bouncy Castle
        java.security.cert.X509Certificate cert = generateSelfSignedCertificate(keyPair);

        // Create KeyStore
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        keyStore.setKeyEntry("alias", keyPair.getPrivate(), "".toCharArray(),
                new java.security.cert.Certificate[] { cert });
        return keyStore;
    }

    private java.security.cert.X509Certificate generateSelfSignedCertificate(java.security.KeyPair keyPair)
            throws Exception {
        org.bouncycastle.cert.X509v3CertificateBuilder builder = new org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder(
                new org.bouncycastle.asn1.x500.X500Name("CN=Test"),
                java.math.BigInteger.valueOf(System.currentTimeMillis()),
                java.util.Date.from(java.time.Instant.now()),
                java.util.Date.from(java.time.Instant.now().plus(365, java.time.temporal.ChronoUnit.DAYS)),
                new org.bouncycastle.asn1.x500.X500Name("CN=Test"),
                keyPair.getPublic());

        org.bouncycastle.operator.ContentSigner signer = new org.bouncycastle.operator.jcajce.JcaContentSignerBuilder(
                "SHA256WithRSA")
                .build(keyPair.getPrivate());

        return new org.bouncycastle.cert.jcajce.JcaX509CertificateConverter().getCertificate(builder.build(signer));
    }
}
