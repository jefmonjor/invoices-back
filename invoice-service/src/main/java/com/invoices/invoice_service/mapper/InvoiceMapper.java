package com.invoices.invoice_service.mapper;

import com.invoices.invoice_service.dto.InvoiceDTO;
import com.invoices.invoice_service.entity.Invoice;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface InvoiceMapper {
    InvoiceDTO toDTO(Invoice invoice);
    Invoice toEntity(InvoiceDTO invoiceDTO);
    void updateEntity(InvoiceDTO invoiceDTO, @MappingTarget Invoice invoice);
}
