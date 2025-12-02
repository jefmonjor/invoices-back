package com.invoices.verifactu.domain.ports;

import com.invoices.invoice.domain.entities.Client;
import com.invoices.invoice.domain.entities.Company;
import com.invoices.invoice.domain.entities.Invoice;
import com.invoices.verifactu.domain.model.AeatResponse;
import com.invoices.verifactu.domain.model.VerifactuMode;
import com.invoices.verifactu.domain.model.VerifactuResponse;

import java.security.KeyStore;

/**
 * Port for Veri*Factu integration with AEAT.
 * This is a domain interface that will be implemented by infrastructure layer.
 * Keeps domain layer independent of specific AEAT integration details.
 *
 * Handles:
 * - XML building and signing
 * - Communication with AEAT endpoint
 * - Response parsing
 */
public interface VerifactuIntegrationPort {

    /**
     * Builds canonical XML for an invoice.
     *
     * @param invoice the invoice to build XML for
     * @param company the company (sender)
     * @param client the client (receiver)
     * @return XML string
     */
    String buildCanonicalXML(Invoice invoice, Company company, Client client);

    /**
     * Signs XML with a certificate.
     *
     * @param xml the XML to sign
     * @param keyStore the KeyStore containing the signing certificate
     * @param password the KeyStore password
     * @return signed XML string
     * @throws Exception if signing fails
     */
    String signXML(String xml, KeyStore keyStore, String password) throws Exception;

    /**
     * Calls the AEAT endpoint with signed XML.
     *
     * @param signedXml the signed XML to send
     * @param mode the Verifactu mode (SANDBOX, PRODUCTION)
     * @return raw AEAT response
     * @throws Exception if communication fails
     */
    AeatResponse callAEATEndpoint(String signedXml, VerifactuMode mode) throws Exception;

    /**
     * Parses AEAT response.
     *
     * @param rawResponse the raw AEAT response
     * @return parsed VerifactuResponse
     */
    VerifactuResponse parseResponse(AeatResponse rawResponse);
}
