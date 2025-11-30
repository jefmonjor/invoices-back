package com.invoices.invoice.domain.entities;

import com.invoices.invoice.domain.exceptions.InvalidInvoiceNumberFormatException;
import com.invoices.invoice.domain.exceptions.InvalidInvoiceStateException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Invoice domain entity - Core business object.
 * Contains business logic and validations.
 * NO dependencies on frameworks (JPA, Spring, etc.).
 */
public class Invoice {
    // Accept formats matching frontend: letters (upper/lower), numbers, hyphens,
    // dots
    // Examples: FacturaA057.pdf, 4592JBZ-SEP-25.pdf, INV-2025-001, 047/2025,
    // A057/2025
    private static final Pattern INVOICE_NUMBER_PATTERN = Pattern.compile("^[A-Za-z0-9./-]+$");
    private static final int DECIMAL_SCALE = 2;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final Long id;
    private final Long companyId; // ID de la empresa emisora
    private Long clientId; // ID del cliente
    private Company company; // Datos completos del emisor (opcional, para PDFs)
    private Client client; // Datos completos del cliente (opcional, para PDFs)
    private String invoiceNumber; // Mutable - can be updated
    private String settlementNumber; // Número de liquidación (opcional)
    private final LocalDateTime issueDate;
    private final List<InvoiceItem> items;
    private BigDecimal irpfPercentage;
    private BigDecimal rePercentage;
    private InvoiceStatus status;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // VeriFactu fields
    private String documentHash;
    private String canonicalJson;
    private String previousDocumentHash;
    private String pdfServerPath;
    private String verifactuStatus;
    private String documentJson;
    private String verifactuTxId;
    private String verifactuRawResponse;
    private Boolean pdfIsFinal;
    private String qrPayload;
    private String hash;
    private String lastHashBefore;
    private String csvAcuse;
    private String qrData;
    private String xmlContent;
    private boolean isRectificativa;
    private Long rectifiesInvoiceId;

    public Invoice(
            Long id,
            Long companyId,
            Long clientId,
            String invoiceNumber,
            LocalDateTime issueDate,
            BigDecimal irpfPercentage,
            BigDecimal rePercentage) {
        validateInvoiceNumber(invoiceNumber);
        validatePercentages(irpfPercentage, rePercentage);

        this.id = id;
        this.companyId = companyId;
        this.clientId = clientId;
        this.invoiceNumber = invoiceNumber;
        this.issueDate = issueDate;
        this.items = new ArrayList<>();
        this.irpfPercentage = irpfPercentage != null
                ? irpfPercentage.setScale(DECIMAL_SCALE, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        this.rePercentage = rePercentage != null
                ? rePercentage.setScale(DECIMAL_SCALE, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        this.status = InvoiceStatus.DRAFT;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void addItem(InvoiceItem item) {
        if (status == InvoiceStatus.FINALIZED || status == InvoiceStatus.PAID) {
            throw new InvalidInvoiceStateException(
                    "Cannot modify invoice in " + status.getDisplayName() + " status");
        }
        items.add(item);
        updateTimestamp();
    }

    /**
     * Adds an item without validating invoice state.
     * FOR INTERNAL USE ONLY - Used by persistence layer when reconstructing
     * entities from database.
     * DO NOT use this method in business logic.
     * WARNING: Bypasses all state validations. Only use in mapper/reconstruction
     * context.
     *
     * @param item the item to add
     */
    public void addItemInternal(InvoiceItem item) {
        items.add(item);
    }

    public void removeItem(InvoiceItem item) {
        if (status == InvoiceStatus.FINALIZED || status == InvoiceStatus.PAID) {
            throw new InvalidInvoiceStateException(
                    "Cannot modify invoice in " + status.getDisplayName() + " status");
        }
        items.remove(item);
        updateTimestamp();
    }

    /**
     * Clears all items from the invoice.
     * Used when updating invoice with a new set of items.
     * Cannot be called on finalized or paid invoices.
     */
    public void clearItems() {
        if (status == InvoiceStatus.FINALIZED || status == InvoiceStatus.PAID) {
            throw new InvalidInvoiceStateException(
                    "Cannot modify invoice in " + status.getDisplayName() + " status");
        }
        items.clear();
        updateTimestamp();
    }

    /**
     * Marks invoice as pending.
     * Valid transition: DRAFT → PENDING
     */
    public void markAsPending() {
        if (status != InvoiceStatus.DRAFT) {
            throw new InvalidInvoiceStateException(
                    "Can only mark draft invoices as pending. Current status: " + status.getDisplayName());
        }
        if (items.isEmpty()) {
            throw new InvalidInvoiceStateException(
                    "Cannot mark invoice as pending without items");
        }
        this.status = InvoiceStatus.PENDING;
        updateTimestamp();
    }

    /**
     * Marks invoice as finalized.
     * Valid transition: PENDING → FINALIZED
     */
    public void markAsFinalized() {
        if (status != InvoiceStatus.PENDING) {
            throw new InvalidInvoiceStateException(
                    "Can only finalize pending invoices. Current status: " + status.getDisplayName());
        }
        this.status = InvoiceStatus.FINALIZED;
        updateTimestamp();
    }

    /**
     * Marks invoice as paid.
     * Valid transitions: PENDING → PAID or FINALIZED → PAID
     */
    public void markAsPaid() {
        if (status != InvoiceStatus.PENDING && status != InvoiceStatus.FINALIZED) {
            throw new InvalidInvoiceStateException(
                    "Can only mark pending or finalized invoices as paid. Current status: " + status.getDisplayName());
        }
        this.status = InvoiceStatus.PAID;
        updateTimestamp();
    }

    public void cancel() {
        if (status == InvoiceStatus.PAID || status == InvoiceStatus.SENT) {
            throw new InvalidInvoiceStateException(
                    "Cannot cancel paid or sent invoices");
        }
        this.status = InvoiceStatus.CANCELLED;
        updateTimestamp();
    }

    public void markAsSending() {
        if (status != InvoiceStatus.PENDING && status != InvoiceStatus.FINALIZED) {
            throw new InvalidInvoiceStateException(
                    "Can only send pending or finalized invoices. Current status: " + status.getDisplayName());
        }
        this.status = InvoiceStatus.SENDING;
        updateTimestamp();
    }

    public void markAsSent() {
        if (status != InvoiceStatus.SENDING) {
            throw new InvalidInvoiceStateException(
                    "Can only mark as sent from sending status. Current status: " + status.getDisplayName());
        }
        this.status = InvoiceStatus.SENT;
        updateTimestamp();
    }

    public void markAsRejected() {
        if (status != InvoiceStatus.SENDING) {
            throw new InvalidInvoiceStateException(
                    "Can only mark as rejected from sending status. Current status: " + status.getDisplayName());
        }
        this.status = InvoiceStatus.REJECTED;
        updateTimestamp();
    }

    public BigDecimal calculateBaseAmount() {
        return items.stream()
                .map(InvoiceItem::calculateSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(DECIMAL_SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateIrpfAmount() {
        BigDecimal baseAmount = calculateBaseAmount();
        return baseAmount.multiply(irpfPercentage)
                .divide(ONE_HUNDRED, DECIMAL_SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateReAmount() {
        BigDecimal baseAmount = calculateBaseAmount();
        return baseAmount.multiply(rePercentage)
                .divide(ONE_HUNDRED, DECIMAL_SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateTotalAmount() {
        BigDecimal itemsTotal = items.stream()
                .map(InvoiceItem::calculateTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal irpfAmount = calculateIrpfAmount();
        BigDecimal reAmount = calculateReAmount();

        return itemsTotal.subtract(irpfAmount).add(reAmount)
                .setScale(DECIMAL_SCALE, RoundingMode.HALF_UP);
    }

    public void setNotes(String notes) {
        this.notes = notes;
        updateTimestamp();
    }

    public void setSettlementNumber(String settlementNumber) {
        this.settlementNumber = settlementNumber;
        updateTimestamp();
    }

    /**
     * Sets the invoice number.
     * Validates the format and updates the invoice number.
     * This field is now mutable to support business requirements.
     *
     * @param invoiceNumber the new invoice number
     */
    public void setInvoiceNumber(String invoiceNumber) {
        validateInvoiceNumber(invoiceNumber);
        this.invoiceNumber = invoiceNumber;
        updateTimestamp();
    }

    /**
     * Sets the invoice status without validation.
     * FOR INTERNAL USE ONLY - Used by persistence layer when reconstructing
     * entities from database.
     * DO NOT use this method in business logic. Use markAsPending(), markAsPaid(),
     * cancel() instead.
     * WARNING: Bypasses all state transition validations. Only use in
     * mapper/reconstruction context.
     *
     * @param status the status to set
     */
    public void setStatusInternal(InvoiceStatus status) {
        this.status = status;
    }

    /**
     * Sets timestamps without validation.
     * FOR INTERNAL USE ONLY - Used by persistence layer when reconstructing
     * entities from database.
     * WARNING: Directly sets timestamps from database. Only use in
     * mapper/reconstruction context.
     *
     * @param createdAt the creation timestamp
     * @param updatedAt the update timestamp
     */
    public void setTimestampsInternal(LocalDateTime createdAt, LocalDateTime updatedAt) {
        if (createdAt != null && updatedAt != null) {
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }
    }

    /**
     * Sets notes without updating timestamp.
     * FOR INTERNAL USE ONLY - Used by persistence layer when reconstructing
     * entities from database.
     * WARNING: Does not update timestamp. Only use in mapper/reconstruction
     * context.
     *
     * @param notes the notes to set
     */
    public void setNotesInternal(String notes) {
        this.notes = notes;
    }

    /**
     * Sets settlement number without updating timestamp.
     * FOR INTERNAL USE ONLY - Used by persistence layer when reconstructing
     * entities from database.
     * WARNING: Does not update timestamp. Only use in mapper/reconstruction
     * context.
     *
     * @param settlementNumber the settlement number to set
     */
    public void setSettlementNumberInternal(String settlementNumber) {
        this.settlementNumber = settlementNumber;
    }

    private void validateInvoiceNumber(String number) {
        if (number == null || number.trim().isEmpty()) {
            throw new InvalidInvoiceNumberFormatException("null or empty");
        }
        if (!INVOICE_NUMBER_PATTERN.matcher(number).matches()) {
            throw new InvalidInvoiceNumberFormatException(number);
        }
    }

    private void validatePercentages(BigDecimal irpf, BigDecimal re) {
        if (irpf != null) {
            if (irpf.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("IRPF percentage cannot be negative");
            }
            if (irpf.compareTo(ONE_HUNDRED) > 0) {
                throw new IllegalArgumentException("IRPF percentage cannot exceed 100%");
            }
        }
        if (re != null) {
            if (re.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("RE percentage cannot be negative");
            }
            if (re.compareTo(ONE_HUNDRED) > 0) {
                throw new IllegalArgumentException("RE percentage cannot exceed 100%");
            }
        }
    }

    private void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    /**
     * Sets the client ID.
     * Updates the clientId reference. Does not validate the client exists.
     * Caller should ensure the client exists before calling this method.
     *
     * @param clientId the new client ID
     */
    public void setClientId(Long clientId) {
        if (clientId == null) {
            throw new IllegalArgumentException("Client ID cannot be null");
        }
        this.clientId = clientId;
        updateTimestamp();
    }

    /**
     * Sets the IRPF percentage.
     * Validates percentage is between 0 and 100.
     * This will affect invoice calculations.
     *
     * @param irpfPercentage the new IRPF percentage
     */
    public void setIrpfPercentage(BigDecimal irpfPercentage) {
        validatePercentages(irpfPercentage, null);
        this.irpfPercentage = irpfPercentage != null
                ? irpfPercentage.setScale(DECIMAL_SCALE, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        updateTimestamp();
    }

    /**
     * Sets the RE percentage.
     * Validates percentage is between 0 and 100.
     * This will affect invoice calculations.
     *
     * @param rePercentage the new RE percentage
     */
    public void setRePercentage(BigDecimal rePercentage) {
        validatePercentages(null, rePercentage);
        this.rePercentage = rePercentage != null
                ? rePercentage.setScale(DECIMAL_SCALE, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        updateTimestamp();
    }

    // Getters (no setters - prefer immutability)
    public Long getId() {
        return id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public Long getClientId() {
        return clientId;
    }

    public Company getCompany() {
        return company;
    }

    public Client getClient() {
        return client;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public String getSettlementNumber() {
        return settlementNumber;
    }

    public LocalDateTime getIssueDate() {
        return issueDate;
    }

    public List<InvoiceItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public BigDecimal getIrpfPercentage() {
        return irpfPercentage;
    }

    public BigDecimal getRePercentage() {
        return rePercentage;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public String getNotes() {
        return notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // VeriFactu getters/setters
    public String getDocumentHash() {
        return documentHash;
    }

    public void setDocumentHash(String documentHash) {
        this.documentHash = documentHash;
        updateTimestamp();
    }

    public String getPdfServerPath() {
        return pdfServerPath;
    }

    public void setPdfServerPath(String pdfServerPath) {
        this.pdfServerPath = pdfServerPath;
        updateTimestamp();
    }

    public String getVerifactuStatus() {
        return verifactuStatus;
    }

    public void setVerifactuStatus(String verifactuStatus) {
        this.verifactuStatus = verifactuStatus;
        updateTimestamp();
    }

    public String getDocumentJson() {
        return documentJson;
    }

    public void setDocumentJson(String documentJson) {
        this.documentJson = documentJson;
        updateTimestamp();
    }

    public String getVerifactuTxId() {
        return verifactuTxId;
    }

    public void setVerifactuTxId(String verifactuTxId) {
        this.verifactuTxId = verifactuTxId;
        updateTimestamp();
    }

    public String getVerifactuRawResponse() {
        return verifactuRawResponse;
    }

    public void setVerifactuRawResponse(String verifactuRawResponse) {
        this.verifactuRawResponse = verifactuRawResponse;
        updateTimestamp();
    }

    public Boolean getPdfIsFinal() {
        return pdfIsFinal;
    }

    public void setPdfIsFinal(Boolean pdfIsFinal) {
        this.pdfIsFinal = pdfIsFinal;
        updateTimestamp();
    }

    public String getQrPayload() {
        return qrPayload;
    }

    public void setQrPayload(String qrPayload) {
        this.qrPayload = qrPayload;
        updateTimestamp();
    }

    public String getCanonicalJson() {
        return canonicalJson;
    }

    public void setCanonicalJson(String canonicalJson) {
        this.canonicalJson = canonicalJson;
        updateTimestamp();
    }

    public String getPreviousDocumentHash() {
        return previousDocumentHash;
    }

    public void setPreviousDocumentHash(String previousDocumentHash) {
        this.previousDocumentHash = previousDocumentHash;
        updateTimestamp();
    }

    public BigDecimal getTotalAmount() {
        return calculateTotalAmount();
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
        updateTimestamp();
    }

    public String getLastHashBefore() {
        return lastHashBefore;
    }

    public void setLastHashBefore(String lastHashBefore) {
        this.lastHashBefore = lastHashBefore;
        updateTimestamp();
    }

    public String getCsvAcuse() {
        return csvAcuse;
    }

    public void setCsvAcuse(String csvAcuse) {
        this.csvAcuse = csvAcuse;
        updateTimestamp();
    }

    public String getQrData() {
        return qrData;
    }

    public void setQrData(String qrData) {
        this.qrData = qrData;
        updateTimestamp();
    }

    public String getXmlContent() {
        return xmlContent;
    }

    public void setXmlContent(String xmlContent) {
        this.xmlContent = xmlContent;
        updateTimestamp();
    }

    public boolean isRectificativa() {
        return isRectificativa;
    }

    public void setRectificativa(boolean rectificativa) {
        isRectificativa = rectificativa;
        updateTimestamp();
    }

    public Long getRectifiesInvoiceId() {
        return rectifiesInvoiceId;
    }

    public void setRectifiesInvoiceId(Long rectifiesInvoiceId) {
        this.rectifiesInvoiceId = rectifiesInvoiceId;
        updateTimestamp();
    }
}
