package com.invoices.invoice.infrastructure.persistence.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity for invoice persistence.
 * This is NOT a domain entity - it's an infrastructure concern.
 * Separated from domain to follow Clean Architecture.
 */
@Entity
@Table(name = "invoices")
public class InvoiceJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(name = "invoice_number", nullable = false, unique = true, length = 50)
    private String invoiceNumber;

    @Column(name = "settlement_number", length = 50)
    private String settlementNumber;

    @Column(name = "issue_date", nullable = false)
    private LocalDateTime issueDate;

    @Column(name = "base_amount", precision = 10, scale = 2)
    private BigDecimal baseAmount;

    @Column(name = "irpf_percentage", precision = 5, scale = 2)
    private BigDecimal irpfPercentage;

    @Column(name = "irpf_amount", precision = 10, scale = 2)
    private BigDecimal irpfAmount;

    @Column(name = "re_percentage", precision = 5, scale = 2)
    private BigDecimal rePercentage;

    @Column(name = "re_amount", precision = 10, scale = 2)
    private BigDecimal reAmount;

    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<InvoiceItemJpaEntity> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // VERI*FACTU fields
    @Column(name = "document_json", columnDefinition = "TEXT")
    private String documentJson;

    @Column(name = "canonical_json", columnDefinition = "TEXT")
    private String canonicalJson;

    @Column(name = "document_hash", length = 64)
    private String documentHash;

    @Column(name = "previous_document_hash", length = 64)
    private String previousDocumentHash;

    @Column(name = "verifactu_status", length = 50)
    private String verifactuStatus;

    @Column(name = "verifactu_tx_id", length = 255)
    private String verifactuTxId;

    @Column(name = "verifactu_raw_response", columnDefinition = "TEXT")
    private String verifactuRawResponse;

    @Column(name = "pdf_server_path", length = 500)
    private String pdfServerPath;

    @Column(name = "pdf_is_final")
    private Boolean pdfIsFinal;

    @Column(name = "qr_payload", columnDefinition = "TEXT")
    private String qrPayload;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors
    public InvoiceJpaEntity() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public String getSettlementNumber() {
        return settlementNumber;
    }

    public void setSettlementNumber(String settlementNumber) {
        this.settlementNumber = settlementNumber;
    }

    public LocalDateTime getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDateTime issueDate) {
        this.issueDate = issueDate;
    }

    public BigDecimal getBaseAmount() {
        return baseAmount;
    }

    public void setBaseAmount(BigDecimal baseAmount) {
        this.baseAmount = baseAmount;
    }

    public BigDecimal getIrpfPercentage() {
        return irpfPercentage;
    }

    public void setIrpfPercentage(BigDecimal irpfPercentage) {
        this.irpfPercentage = irpfPercentage;
    }

    public BigDecimal getIrpfAmount() {
        return irpfAmount;
    }

    public void setIrpfAmount(BigDecimal irpfAmount) {
        this.irpfAmount = irpfAmount;
    }

    public BigDecimal getRePercentage() {
        return rePercentage;
    }

    public void setRePercentage(BigDecimal rePercentage) {
        this.rePercentage = rePercentage;
    }

    public BigDecimal getReAmount() {
        return reAmount;
    }

    public void setReAmount(BigDecimal reAmount) {
        this.reAmount = reAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<InvoiceItemJpaEntity> getItems() {
        return items;
    }

    public void setItems(List<InvoiceItemJpaEntity> items) {
        this.items = items;
    }

    public void addItem(InvoiceItemJpaEntity item) {
        items.add(item);
        item.setInvoice(this);
    }

    public void removeItem(InvoiceItemJpaEntity item) {
        items.remove(item);
        item.setInvoice(null);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // VERI*FACTU Getters and Setters
    public String getDocumentJson() {
        return documentJson;
    }

    public void setDocumentJson(String documentJson) {
        this.documentJson = documentJson;
    }

    public String getCanonicalJson() {
        return canonicalJson;
    }

    public void setCanonicalJson(String canonicalJson) {
        this.canonicalJson = canonicalJson;
    }

    public String getDocumentHash() {
        return documentHash;
    }

    public void setDocumentHash(String documentHash) {
        this.documentHash = documentHash;
    }

    public String getPreviousDocumentHash() {
        return previousDocumentHash;
    }

    public void setPreviousDocumentHash(String previousDocumentHash) {
        this.previousDocumentHash = previousDocumentHash;
    }

    public String getVerifactuStatus() {
        return verifactuStatus;
    }

    public void setVerifactuStatus(String verifactuStatus) {
        this.verifactuStatus = verifactuStatus;
    }

    public String getVerifactuTxId() {
        return verifactuTxId;
    }

    public void setVerifactuTxId(String verifactuTxId) {
        this.verifactuTxId = verifactuTxId;
    }

    public String getVerifactuRawResponse() {
        return verifactuRawResponse;
    }

    public void setVerifactuRawResponse(String verifactuRawResponse) {
        this.verifactuRawResponse = verifactuRawResponse;
    }

    public String getPdfServerPath() {
        return pdfServerPath;
    }

    public void setPdfServerPath(String pdfServerPath) {
        this.pdfServerPath = pdfServerPath;
    }

    public Boolean getPdfIsFinal() {
        return pdfIsFinal;
    }

    public void setPdfIsFinal(Boolean pdfIsFinal) {
        this.pdfIsFinal = pdfIsFinal;
    }

    public String getQrPayload() {
        return qrPayload;
    }

    public void setQrPayload(String qrPayload) {
        this.qrPayload = qrPayload;
    }
}
