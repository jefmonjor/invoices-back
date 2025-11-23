package com.invoices.invoice.infrastructure.verifactu;

import com.invoices.invoice.domain.entities.Invoice;
import com.invoices.invoice.domain.entities.InvoiceItem;
import com.invoices.company.domain.entities.Company;
import com.invoices.client.domain.entities.Client;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Builder for VERI*FACTU XML documents according to the urn:verifactu:invoices:1.0 schema.
 * This builder creates invoice transmission XML with canonical hashing and invoice chaining.
 */
@Service
@Slf4j
public class VerifactuXmlBuilder {

    private static final String VERIFACTU_NAMESPACE = "urn:verifactu:invoices:1.0";
    private static final DateTimeFormatter ISO_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter ISO_DATETIME_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    @Value("${verifactu.webhook.url:https://api.example.com/webhook/verifactu}")
    private String webhookUrl;

    /**
     * Builds a VERI*FACTU compliant XML document for invoice transmission.
     *
     * @param invoice         The invoice to transmit
     * @param company         The issuing company
     * @param client          The recipient client
     * @param canonicalHash   The SHA-256 canonical hash of the invoice (64 hex chars)
     * @param previousHash    The hash of the previous invoice in the chain (or empty string if first)
     * @return XML Document ready for transmission
     * @throws ParserConfigurationException if XML parser cannot be configured
     */
    public Document buildVerifactuXml(
            Invoice invoice,
            Company company,
            Client client,
            String canonicalHash,
            String previousHash
    ) throws ParserConfigurationException {

        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        docFactory.setNamespaceAware(true);
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();

        // Root element: InvoiceTransmission
        Element rootElement = doc.createElementNS(VERIFACTU_NAMESPACE, "InvoiceTransmission");
        doc.appendChild(rootElement);

        // Build Header
        buildHeader(doc, rootElement, company, previousHash);

        // Build Invoice
        buildInvoice(doc, rootElement, invoice, company, client, canonicalHash, previousHash);

        log.info("Built VERI*FACTU XML for invoice: {}", invoice.getInvoiceNumber());
        return doc;
    }

    /**
     * Builds the Header section of the VERI*FACTU XML.
     */
    private void buildHeader(Document doc, Element root, Company company, String relatedHash) {
        Element header = doc.createElement("Header");
        root.appendChild(header);

        // Sender
        Element sender = doc.createElement("Sender");
        header.appendChild(sender);

        appendTextElement(doc, sender, "TaxId", company.getTaxId());
        appendTextElement(doc, sender, "Name", company.getBusinessName());
        appendTextElement(doc, sender, "EndpointUrl", webhookUrl);

        // TransmissionDate (current timestamp in ISO-8601 format)
        String transmissionDate = ZonedDateTime.now().format(ISO_DATETIME_FORMATTER);
        appendTextElement(doc, header, "TransmissionDate", transmissionDate);

        // MessageId (unique identifier: msg-{timestamp}-{UUID})
        String messageId = generateMessageId();
        appendTextElement(doc, header, "MessageId", messageId);

        // RelatedHash (hash of previous invoice or empty)
        appendTextElement(doc, header, "RelatedHash", relatedHash != null ? relatedHash : "");
    }

    /**
     * Builds the Invoice section of the VERI*FACTU XML.
     */
    private void buildInvoice(
            Document doc,
            Element root,
            Invoice invoice,
            Company company,
            Client client,
            String canonicalHash,
            String previousHash
    ) {
        Element invoiceElement = doc.createElement("Invoice");
        root.appendChild(invoiceElement);

        // Basic invoice data
        appendTextElement(doc, invoiceElement, "InvoiceNumber", invoice.getInvoiceNumber());
        appendTextElement(doc, invoiceElement, "IssueDate", invoice.getIssueDate().format(ISO_DATE_FORMATTER));

        // Issuer (company)
        buildIssuer(doc, invoiceElement, company);

        // Recipient (client)
        buildRecipient(doc, invoiceElement, client);

        // Invoice Lines
        buildLines(doc, invoiceElement, invoice);

        // Totals
        buildTotals(doc, invoiceElement, invoice);

        // Canonical Document Hash (SHA-256, 64 hex chars)
        appendTextElement(doc, invoiceElement, "CanonicalDocumentHash", canonicalHash);

        // Set algorithm attribute
        Element hashElement = (Element) invoiceElement.getElementsByTagName("CanonicalDocumentHash").item(0);
        hashElement.setAttribute("algorithm", "SHA-256");

        // Previous Canonical Hash (for invoice chaining)
        appendTextElement(doc, invoiceElement, "PreviousCanonicalHash", previousHash != null ? previousHash : "");

        // Currency (default EUR)
        appendTextElement(doc, invoiceElement, "Currency", "EUR");

        // Notes (if any)
        String notes = invoice.getNotes() != null ? invoice.getNotes() : "";
        if (!notes.isEmpty()) {
            appendTextElement(doc, invoiceElement, "Notes", notes);
        }
    }

    /**
     * Builds the Issuer section.
     */
    private void buildIssuer(Document doc, Element invoiceElement, Company company) {
        Element issuer = doc.createElement("Issuer");
        invoiceElement.appendChild(issuer);

        appendTextElement(doc, issuer, "TaxId", company.getTaxId());
        appendTextElement(doc, issuer, "LegalName", company.getBusinessName());

        // Build full address from company data
        String fullAddress = buildCompanyAddress(company);
        appendTextElement(doc, issuer, "Address", fullAddress);
    }

    /**
     * Builds the Recipient section.
     */
    private void buildRecipient(Document doc, Element invoiceElement, Client client) {
        Element recipient = doc.createElement("Recipient");
        invoiceElement.appendChild(recipient);

        appendTextElement(doc, recipient, "TaxId", client.getTaxId() != null ? client.getTaxId() : "");
        appendTextElement(doc, recipient, "LegalName", client.getName());

        // Build full address from client data
        String fullAddress = buildClientAddress(client);
        appendTextElement(doc, recipient, "Address", fullAddress);
    }

    /**
     * Builds the Lines section with all invoice line items.
     */
    private void buildLines(Document doc, Element invoiceElement, Invoice invoice) {
        Element lines = doc.createElement("Lines");
        invoiceElement.appendChild(lines);

        int lineNumber = 1;
        for (InvoiceItem item : invoice.getItems()) {
            Element line = doc.createElement("Line");
            lines.appendChild(line);

            appendTextElement(doc, line, "LineNumber", String.valueOf(lineNumber++));
            appendTextElement(doc, line, "Description", item.getDescription());
            appendTextElement(doc, line, "Quantity", formatDecimal(item.getQuantity(), 2));
            appendTextElement(doc, line, "UnitPrice", formatDecimal(item.getPrice(), 2));

            // Discount percentage
            BigDecimal discountPercent = item.getDiscount() != null ? item.getDiscount() : BigDecimal.ZERO;
            appendTextElement(doc, line, "DiscountPercent", formatDecimal(discountPercent, 2));

            // Taxable base (subtotal after discounts)
            BigDecimal taxableBase = item.calculateSubtotal();
            appendTextElement(doc, line, "TaxableBase", formatDecimal(taxableBase, 2));

            // VAT
            BigDecimal vatPercent = item.getVatPercentage() != null ? item.getVatPercentage() : BigDecimal.ZERO;
            appendTextElement(doc, line, "VATPercent", formatDecimal(vatPercent, 2));

            BigDecimal vatAmount = taxableBase.multiply(vatPercent)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            appendTextElement(doc, line, "VATAmount", formatDecimal(vatAmount, 2));

            // Line total (base + VAT)
            BigDecimal lineTotal = taxableBase.add(vatAmount);
            appendTextElement(doc, line, "LineTotal", formatDecimal(lineTotal, 2));
        }
    }

    /**
     * Builds the Totals section with invoice summary.
     */
    private void buildTotals(Document doc, Element invoiceElement, Invoice invoice) {
        Element totals = doc.createElement("Totals");
        invoiceElement.appendChild(totals);

        BigDecimal totalTaxable = BigDecimal.ZERO;
        BigDecimal totalVAT = BigDecimal.ZERO;

        // Calculate totals from invoice items
        for (InvoiceItem item : invoice.getItems()) {
            BigDecimal taxableBase = item.calculateSubtotal();
            BigDecimal vatPercent = item.getVatPercentage() != null ? item.getVatPercentage() : BigDecimal.ZERO;
            BigDecimal vatAmount = taxableBase.multiply(vatPercent)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

            totalTaxable = totalTaxable.add(taxableBase);
            totalVAT = totalVAT.add(vatAmount);
        }

        BigDecimal grandTotal = totalTaxable.add(totalVAT);

        appendTextElement(doc, totals, "TotalTaxable", formatDecimal(totalTaxable, 2));
        appendTextElement(doc, totals, "TotalVAT", formatDecimal(totalVAT, 2));
        appendTextElement(doc, totals, "TotalIRPF", formatDecimal(BigDecimal.ZERO, 2));
        appendTextElement(doc, totals, "TotalRecargoEquivalencia", formatDecimal(BigDecimal.ZERO, 2));
        appendTextElement(doc, totals, "GrandTotal", formatDecimal(grandTotal, 2));
    }

    /**
     * Generates a unique message ID in the format: msg-{timestamp}-{short-UUID}
     */
    private String generateMessageId() {
        long timestamp = System.currentTimeMillis();
        String shortUuid = UUID.randomUUID().toString().substring(0, 8);
        return String.format("msg-%d-%s", timestamp, shortUuid);
    }

    /**
     * Builds a full address string from company data.
     */
    private String buildCompanyAddress(Company company) {
        StringBuilder address = new StringBuilder();

        if (company.getAddress() != null && !company.getAddress().isEmpty()) {
            address.append(company.getAddress());
        }

        if (company.getCity() != null && !company.getCity().isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(company.getCity());
        }

        if (company.getPostalCode() != null && !company.getPostalCode().isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(company.getPostalCode());
        }

        if (company.getCountry() != null && !company.getCountry().isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(company.getCountry());
        }

        return address.length() > 0 ? address.toString() : "N/A";
    }

    /**
     * Builds a full address string from client data.
     */
    private String buildClientAddress(Client client) {
        StringBuilder address = new StringBuilder();

        if (client.getAddress() != null && !client.getAddress().isEmpty()) {
            address.append(client.getAddress());
        }

        if (client.getCity() != null && !client.getCity().isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(client.getCity());
        }

        if (client.getPostalCode() != null && !client.getPostalCode().isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(client.getPostalCode());
        }

        if (client.getCountry() != null && !client.getCountry().isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(client.getCountry());
        }

        return address.length() > 0 ? address.toString() : "N/A";
    }

    /**
     * Formats a BigDecimal to string with specified decimal places.
     */
    private String formatDecimal(BigDecimal value, int decimals) {
        if (value == null) {
            value = BigDecimal.ZERO;
        }
        return value.setScale(decimals, RoundingMode.HALF_UP).toPlainString();
    }

    /**
     * Helper method to append a text element to a parent element.
     */
    private void appendTextElement(Document doc, Element parent, String tagName, String textContent) {
        Element element = doc.createElement(tagName);
        element.appendChild(doc.createTextNode(textContent != null ? textContent : ""));
        parent.appendChild(element);
    }
}
