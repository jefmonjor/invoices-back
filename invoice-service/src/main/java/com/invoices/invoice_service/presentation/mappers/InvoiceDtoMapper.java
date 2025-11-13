package com.invoices.invoice_service.presentation.mappers;

import com.invoices.api.model.InvoiceDTO;
import com.invoices.api.model.InvoiceItemDTO;
import com.invoices.invoice_service.domain.entities.Invoice;
import com.invoices.invoice_service.domain.entities.InvoiceItem;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.stream.Collectors;

/**
 * Mapper between domain Invoice and API InvoiceDTO.
 * Keeps domain and presentation layers decoupled.
 */
@Component
public class InvoiceDtoMapper {

    public InvoiceDTO toDto(Invoice invoice) {
        if (invoice == null) {
            return null;
        }

        InvoiceDTO dto = new InvoiceDTO();
        dto.setId(invoice.getId().intValue());
        dto.setUserId(invoice.getUserId().intValue());
        dto.setClientId(invoice.getClientId().intValue());
        dto.setInvoiceNumber(invoice.getInvoiceNumber());
        dto.setIssueDate(OffsetDateTime.of(invoice.getIssueDate(), ZoneOffset.UTC));
        dto.setBaseAmount(invoice.calculateBaseAmount().floatValue());
        dto.setIrpfPercentage(invoice.getIrpfPercentage().floatValue());
        dto.setIrpfAmount(invoice.calculateIrpfAmount().floatValue());
        dto.setRePercentage(invoice.getRePercentage().floatValue());
        dto.setReAmount(invoice.calculateReAmount().floatValue());
        dto.setTotalAmount(invoice.calculateTotalAmount().floatValue());
        dto.setStatus(invoice.getStatus().getDisplayName());
        dto.setNotes(invoice.getNotes());
        dto.setCreatedAt(OffsetDateTime.of(invoice.getCreatedAt(), ZoneOffset.UTC));
        dto.setUpdatedAt(OffsetDateTime.of(invoice.getUpdatedAt(), ZoneOffset.UTC));

        dto.setItems(
            invoice.getItems().stream()
                .map(this::toItemDto)
                .collect(Collectors.toList())
        );

        return dto;
    }

    private InvoiceItemDTO toItemDto(InvoiceItem item) {
        InvoiceItemDTO dto = new InvoiceItemDTO();
        dto.setId(item.getId().intValue());
        dto.setInvoiceId(item.getInvoiceId().intValue());
        dto.setDescription(item.getDescription());
        dto.setUnits(item.getUnits());
        dto.setPrice(item.getPrice().floatValue());
        dto.setVatPercentage(item.getVatPercentage().floatValue());
        dto.setDiscountPercentage(item.getDiscountPercentage().floatValue());
        dto.setSubtotal(item.calculateSubtotal().floatValue());
        dto.setTotal(item.calculateTotal().floatValue());
        dto.setCreatedAt(OffsetDateTime.of(item.getCreatedAt(), ZoneOffset.UTC));
        dto.setUpdatedAt(OffsetDateTime.of(item.getUpdatedAt(), ZoneOffset.UTC));
        return dto;
    }
}
