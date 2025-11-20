package com.invoices.invoice.domain.entities;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Invoice item domain entity.
 * Represents a line item in an invoice with business logic.
 * NO framework dependencies (no JPA, no Spring).
 */
public class InvoiceItem {
    private static final int DECIMAL_SCALE = 2;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final Long id;
    private final Long invoiceId;
    private final String description;
    private final int units;
    private final BigDecimal price;
    private final BigDecimal vatPercentage;
    private final BigDecimal discountPercentage;
    // Extended fields for detailed invoices
    private LocalDate itemDate;           // Fecha específica del item (FECHA)
    private String vehiclePlate;          // Matrícula del vehículo (MATRÍCULA)
    private String orderNumber;           // Número de pedido (PEDIDO)
    private String zone;                  // Zona de trabajo (ZONA)
    private BigDecimal gasPercentage;     // Porcentaje de gas (% GAS)
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public InvoiceItem(
        Long id,
        Long invoiceId,
        String description,
        int units,
        BigDecimal price,
        BigDecimal vatPercentage,
        BigDecimal discountPercentage
    ) {
        validateInputs(description, units, price, vatPercentage, discountPercentage);

        this.id = id;
        this.invoiceId = invoiceId;
        this.description = description;
        this.units = units;
        this.price = price.setScale(DECIMAL_SCALE, RoundingMode.HALF_UP);
        this.vatPercentage = vatPercentage.setScale(DECIMAL_SCALE, RoundingMode.HALF_UP);
        this.discountPercentage = discountPercentage.setScale(DECIMAL_SCALE, RoundingMode.HALF_UP);
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public BigDecimal calculateSubtotal() {
        BigDecimal subtotalBeforeDiscount = price.multiply(BigDecimal.valueOf(units));
        BigDecimal discountAmount = calculateDiscountAmount(subtotalBeforeDiscount);
        return subtotalBeforeDiscount.subtract(discountAmount)
            .setScale(DECIMAL_SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateTotal() {
        BigDecimal subtotal = calculateSubtotal();
        BigDecimal vatAmount = calculateVatAmount(subtotal);
        return subtotal.add(vatAmount)
            .setScale(DECIMAL_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateDiscountAmount(BigDecimal amount) {
        return amount.multiply(discountPercentage)
            .divide(ONE_HUNDRED, DECIMAL_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateVatAmount(BigDecimal subtotal) {
        return subtotal.multiply(vatPercentage)
            .divide(ONE_HUNDRED, DECIMAL_SCALE, RoundingMode.HALF_UP);
    }

    private void validateInputs(
        String description,
        int units,
        BigDecimal price,
        BigDecimal vatPercentage,
        BigDecimal discountPercentage
    ) {
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description cannot be null or empty");
        }
        if (units <= 0) {
            throw new IllegalArgumentException("Units must be positive");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be positive");
        }
        if (vatPercentage == null || vatPercentage.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("VAT percentage cannot be negative");
        }
        if (vatPercentage.compareTo(ONE_HUNDRED) > 0) {
            throw new IllegalArgumentException("VAT percentage cannot exceed 100%");
        }
        if (discountPercentage == null || discountPercentage.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Discount percentage cannot be negative");
        }
        if (discountPercentage.compareTo(ONE_HUNDRED) > 0) {
            throw new IllegalArgumentException("Discount percentage cannot exceed 100%");
        }
    }

    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }

    // Setters for extended fields
    public void setItemDate(LocalDate itemDate) {
        this.itemDate = itemDate;
        updateTimestamp();
    }

    public void setVehiclePlate(String vehiclePlate) {
        this.vehiclePlate = vehiclePlate;
        updateTimestamp();
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
        updateTimestamp();
    }

    public void setZone(String zone) {
        this.zone = zone;
        updateTimestamp();
    }

    public void setGasPercentage(BigDecimal gasPercentage) {
        if (gasPercentage != null) {
            if (gasPercentage.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Gas percentage cannot be negative");
            }
            if (gasPercentage.compareTo(ONE_HUNDRED) > 0) {
                throw new IllegalArgumentException("Gas percentage cannot exceed 100%");
            }
            this.gasPercentage = gasPercentage.setScale(DECIMAL_SCALE, RoundingMode.HALF_UP);
        } else {
            this.gasPercentage = null;
        }
        updateTimestamp();
    }

    // Getters (no setters - immutability preferred)
    public Long getId() {
        return id;
    }

    public Long getInvoiceId() {
        return invoiceId;
    }

    public String getDescription() {
        return description;
    }

    public int getUnits() {
        return units;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getVatPercentage() {
        return vatPercentage;
    }

    public BigDecimal getDiscountPercentage() {
        return discountPercentage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // Getters for extended fields
    public LocalDate getItemDate() {
        return itemDate;
    }

    public String getVehiclePlate() {
        return vehiclePlate;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public String getZone() {
        return zone;
    }

    public BigDecimal getGasPercentage() {
        return gasPercentage;
    }
}
