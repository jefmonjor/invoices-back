package com.invoices.invoice_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "invoice_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    /**
     * Calcula el total del item multiplicando unitPrice por quantity
     * @return BigDecimal con el total calculado
     */
    public BigDecimal calculateTotal() {
        if (unitPrice != null && quantity != null) {
            return unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
        return BigDecimal.ZERO;
    }

    /**
     * Pre-persist hook para calcular el total antes de guardar
     */
    @PrePersist
    @PreUpdate
    public void prePersist() {
        this.total = calculateTotal();
    }
}
