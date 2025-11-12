package com.invoices.invoice_service.mapper;

import com.invoices.invoice_service.dto.InvoiceItemDTO;
import com.invoices.invoice_service.entity.InvoiceItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface InvoiceItemMapper {
    @Mapping(source = "invoice.id", target = "invoiceId")
    InvoiceItemDTO toDTO(InvoiceItem invoiceItem);

    @Mapping(target = "invoice", ignore = true)
    InvoiceItem toEntity(InvoiceItemDTO invoiceItemDTO);

    @Mapping(target = "invoice", ignore = true)
    void updateEntity(InvoiceItemDTO invoiceItemDTO, @MappingTarget InvoiceItem invoiceItem);
}
