package com.invoices.invoice_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "invoices")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_number", nullable = false, unique = true, length = 50)
    private String invoiceNumber;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(name = "client_email", nullable = false)
    private String clientEmail;

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal tax;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<InvoiceItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Método helper para agregar items a la factura
     * Mantiene la relación bidireccional
     */
    public void addItem(InvoiceItem item) {
        items.add(item);
        item.setInvoice(this);
    }

    /**
     * Método helper para remover items de la factura
     * Mantiene la relación bidireccional
     */
    public void removeItem(InvoiceItem item) {
        items.remove(item);
        item.setInvoice(null);
    }

    /**
     * Calcula el subtotal sumando todos los items
     */
    public BigDecimal calculateSubtotal() {
        return items.stream()
                .map(InvoiceItem::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calcula el tax (19% del subtotal)
     */
    public BigDecimal calculateTax() {
        return subtotal.multiply(new BigDecimal("0.19"));
    }

    /**
     * Calcula el total (subtotal + tax)
     */
    public BigDecimal calculateTotal() {
        return subtotal.add(tax);
    }
}
