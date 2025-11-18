package com.invoices.invoice.presentation.mappers;

import com.invoices.invoice.domain.entities.Invoice;
import com.invoices.invoice.domain.entities.InvoiceItem;
import com.invoices.invoice.dto.InvoiceDTO;
import com.invoices.invoice.dto.InvoiceItemDTO;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * Mapper between domain Invoice and API InvoiceDTO.
 * Keeps domain and presentation layers decoupled.
 */
@Component
public class InvoiceDtoMapper {

    private final CompanyDtoMapper companyMapper;
    private final ClientDtoMapper clientMapper;

    public InvoiceDtoMapper(CompanyDtoMapper companyMapper, ClientDtoMapper clientMapper) {
        this.companyMapper = companyMapper;
        this.clientMapper = clientMapper;
    }

    public InvoiceDTO toDto(Invoice invoice) {
        if (invoice == null) {
            return null;
        }

        return InvoiceDTO.builder()
            .id(invoice.getId())
            .companyId(invoice.getCompanyId())
            .clientId(invoice.getClientId())
            .company(companyMapper.toDto(invoice.getCompany()))
            .client(clientMapper.toDto(invoice.getClient()))
            .invoiceNumber(invoice.getInvoiceNumber())
            .issueDate(invoice.getIssueDate())
            .baseAmount(invoice.calculateBaseAmount())
            .irpfPercentage(invoice.getIrpfPercentage())
            .irpfAmount(invoice.calculateIrpfAmount())
            .rePercentage(invoice.getRePercentage())
            .reAmount(invoice.calculateReAmount())
            .totalAmount(invoice.calculateTotalAmount())
            .status(invoice.getStatus().getDisplayName())
            .notes(invoice.getNotes())
            .createdAt(invoice.getCreatedAt())
            .updatedAt(invoice.getUpdatedAt())
            .items(invoice.getItems().stream()
                .map(this::toItemDto)
                .collect(Collectors.toList()))
            .build();
    }

    private InvoiceItemDTO toItemDto(InvoiceItem item) {
        return InvoiceItemDTO.builder()
            .id(item.getId())
            .invoiceId(item.getInvoiceId())
            .description(item.getDescription())
            .units(item.getUnits())
            .discountPercentage(item.getDiscountPercentage())
            .price(item.getPrice())
            .vatPercentage(item.getVatPercentage())
            .subtotal(item.calculateSubtotal())
            .total(item.calculateTotal())
            .createdAt(item.getCreatedAt())
            .updatedAt(item.getUpdatedAt())
            .build();
    }
}
