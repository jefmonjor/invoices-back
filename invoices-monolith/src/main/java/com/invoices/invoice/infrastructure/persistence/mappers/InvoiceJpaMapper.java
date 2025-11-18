package com.invoices.invoice.infrastructure.persistence.mappers;

import com.invoices.invoice.domain.entities.Invoice;
import com.invoices.invoice.domain.entities.InvoiceItem;
import com.invoices.invoice.domain.entities.InvoiceStatus;
import com.invoices.invoice.infrastructure.persistence.entities.InvoiceItemJpaEntity;
import com.invoices.invoice.infrastructure.persistence.entities.InvoiceJpaEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper between domain Invoice and JPA InvoiceJpaEntity.
 * Keeps domain and infrastructure layers decoupled.
 */
@Component
public class InvoiceJpaMapper {

    public InvoiceJpaEntity toJpaEntity(Invoice invoice) {
        if (invoice == null) {
            return null;
        }

        InvoiceJpaEntity jpaEntity = new InvoiceJpaEntity();
        jpaEntity.setId(invoice.getId());
        jpaEntity.setCompanyId(invoice.getCompanyId());
        jpaEntity.setClientId(invoice.getClientId());
        jpaEntity.setInvoiceNumber(invoice.getInvoiceNumber());
        jpaEntity.setIssueDate(invoice.getIssueDate());
        jpaEntity.setBaseAmount(invoice.calculateBaseAmount());
        jpaEntity.setIrpfPercentage(invoice.getIrpfPercentage());
        jpaEntity.setIrpfAmount(invoice.calculateIrpfAmount());
        jpaEntity.setRePercentage(invoice.getRePercentage());
        jpaEntity.setReAmount(invoice.calculateReAmount());
        jpaEntity.setTotalAmount(invoice.calculateTotalAmount());
        jpaEntity.setStatus(invoice.getStatus().name());
        jpaEntity.setNotes(invoice.getNotes());
        jpaEntity.setCreatedAt(invoice.getCreatedAt());
        jpaEntity.setUpdatedAt(invoice.getUpdatedAt());

        List<InvoiceItemJpaEntity> itemEntities = invoice.getItems().stream()
            .map(item -> toJpaItemEntity(item, jpaEntity))
            .collect(Collectors.toList());
        jpaEntity.setItems(itemEntities);

        return jpaEntity;
    }

    public Invoice toDomainEntity(InvoiceJpaEntity jpaEntity) {
        if (jpaEntity == null) {
            return null;
        }

        Invoice invoice = new Invoice(
            jpaEntity.getId(),
            jpaEntity.getCompanyId(),
            jpaEntity.getClientId(),
            jpaEntity.getInvoiceNumber(),
            jpaEntity.getIssueDate(),
            jpaEntity.getIrpfPercentage(),
            jpaEntity.getRePercentage()
        );

        // Set status from database without validation (using internal method)
        if (jpaEntity.getStatus() != null) {
            invoice.setStatusInternal(InvoiceStatus.valueOf(jpaEntity.getStatus()));
        }

        // Set notes if present
        if (jpaEntity.getNotes() != null) {
            invoice.setNotes(jpaEntity.getNotes());
        }

        // Set timestamps from database
        if (jpaEntity.getCreatedAt() != null && jpaEntity.getUpdatedAt() != null) {
            invoice.setTimestampsInternal(jpaEntity.getCreatedAt(), jpaEntity.getUpdatedAt());
        }

        // Add items without state validation (using internal method)
        // This is necessary to reconstruct invoices in FINALIZED or PAID status
        jpaEntity.getItems().forEach(itemJpa -> {
            InvoiceItem item = toDomainItemEntity(itemJpa);
            invoice.addItemInternal(item);
        });

        return invoice;
    }

    private InvoiceItemJpaEntity toJpaItemEntity(InvoiceItem item, InvoiceJpaEntity invoiceJpa) {
        InvoiceItemJpaEntity jpaItem = new InvoiceItemJpaEntity();
        jpaItem.setId(item.getId());
        jpaItem.setInvoice(invoiceJpa);
        jpaItem.setDescription(item.getDescription());
        jpaItem.setUnits(item.getUnits());
        jpaItem.setPrice(item.getPrice());
        jpaItem.setVatPercentage(item.getVatPercentage());
        jpaItem.setDiscountPercentage(item.getDiscountPercentage());
        jpaItem.setSubtotal(item.calculateSubtotal());
        jpaItem.setTotal(item.calculateTotal());
        jpaItem.setCreatedAt(item.getCreatedAt());
        jpaItem.setUpdatedAt(item.getUpdatedAt());
        return jpaItem;
    }

    private InvoiceItem toDomainItemEntity(InvoiceItemJpaEntity jpaItem) {
        return new InvoiceItem(
            jpaItem.getId(),
            jpaItem.getInvoice().getId(),
            jpaItem.getDescription(),
            jpaItem.getUnits(),
            jpaItem.getPrice(),
            jpaItem.getVatPercentage(),
            jpaItem.getDiscountPercentage() != null ? jpaItem.getDiscountPercentage() : java.math.BigDecimal.ZERO
        );
    }
}
