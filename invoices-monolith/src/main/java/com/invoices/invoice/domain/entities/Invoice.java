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
    private static final Pattern INVOICE_NUMBER_PATTERN = Pattern.compile("\\d{4}-\\d{3}");
    private static final int DECIMAL_SCALE = 2;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final Long id;
    private final Long companyId;    // ID de la empresa emisora
    private final Long clientId;     // ID del cliente
    private Company company;         // Datos completos del emisor (opcional, para PDFs)
    private Client client;           // Datos completos del cliente (opcional, para PDFs)
    private final String invoiceNumber;
    private final LocalDateTime issueDate;
    private final List<InvoiceItem> items;
    private final BigDecimal irpfPercentage;
    private final BigDecimal rePercentage;
    private InvoiceStatus status;
    private String notes;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Invoice(
        Long id,
        Long companyId,
        Long clientId,
        String invoiceNumber,
        LocalDateTime issueDate,
        BigDecimal irpfPercentage,
        BigDecimal rePercentage
    ) {
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
                "Cannot modify invoice in " + status.getDisplayName() + " status"
            );
        }
        items.add(item);
        updateTimestamp();
    }

    public void removeItem(InvoiceItem item) {
        if (status == InvoiceStatus.FINALIZED || status == InvoiceStatus.PAID) {
            throw new InvalidInvoiceStateException(
                "Cannot modify invoice in " + status.getDisplayName() + " status"
            );
        }
        items.remove(item);
        updateTimestamp();
    }

    public void markAsPending() {
        if (items.isEmpty()) {
            throw new InvalidInvoiceStateException(
                "Cannot mark invoice as pending without items"
            );
        }
        this.status = InvoiceStatus.PENDING;
        updateTimestamp();
    }

    public void markAsPaid() {
        if (status != InvoiceStatus.PENDING) {
            throw new InvalidInvoiceStateException(
                "Can only mark pending invoices as paid"
            );
        }
        this.status = InvoiceStatus.PAID;
        updateTimestamp();
    }

    public void cancel() {
        if (status == InvoiceStatus.PAID) {
            throw new InvalidInvoiceStateException(
                "Cannot cancel paid invoices"
            );
        }
        this.status = InvoiceStatus.CANCELLED;
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

    private void validateInvoiceNumber(String number) {
        if (number == null || number.trim().isEmpty()) {
            throw new InvalidInvoiceNumberFormatException("null or empty");
        }
        if (!INVOICE_NUMBER_PATTERN.matcher(number).matches()) {
            throw new InvalidInvoiceNumberFormatException(number);
        }
    }

    private void validatePercentages(BigDecimal irpf, BigDecimal re) {
        if (irpf != null && irpf.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("IRPF percentage cannot be negative");
        }
        if (re != null && re.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("RE percentage cannot be negative");
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
}
