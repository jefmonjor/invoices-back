package com.invoices.invoice.presentation.mappers;

import com.invoices.invoice.domain.entities.Invoice;
import com.invoices.invoice.domain.entities.InvoiceItem;
import com.invoices.invoice.domain.models.InvoiceSummary;
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
                .settlementNumber(invoice.getSettlementNumber())
                .issueDate(invoice.getIssueDate())
                .baseAmount(invoice.calculateBaseAmount())
                .irpfPercentage(invoice.getIrpfPercentage())
                .irpfAmount(invoice.calculateIrpfAmount())
                .rePercentage(invoice.getRePercentage())
                .reAmount(invoice.calculateReAmount())
                .totalAmount(invoice.calculateTotalAmount())
                .status(invoice.getStatus() != null ? invoice.getStatus().getDisplayName() : null)
                .notes(invoice.getNotes())
                .createdAt(invoice.getCreatedAt())
                .updatedAt(invoice.getUpdatedAt())
                .items(invoice.getItems() != null ? invoice.getItems().stream()
                        .map(this::toItemDto)
                        .collect(Collectors.toList()) : java.util.Collections.emptyList())
                // VeriFactu fields
                .documentHash(invoice.getDocumentHash())
                .pdfServerPath(invoice.getPdfServerPath())
                .verifactuStatus(invoice.getVerifactuStatus())
                .verifactuTxId(invoice.getVerifactuTxId())
                .verifactuError(invoice.getVerifactuError())
                .verifactuRetryCount(invoice.getVerifactuRetryCount())
                .pdfIsFinal(invoice.getPdfIsFinal())
                .build();
    }

    public InvoiceDTO toSummaryDto(InvoiceSummary summary) {
        if (summary == null) {
            return null;
        }

        return InvoiceDTO.builder()
                .id(summary.id())
                .invoiceNumber(summary.invoiceNumber())
                .issueDate(summary.issueDate())
                .totalAmount(summary.totalAmount())
                .status(summary.status())
                .clientId(summary.clientId())
                .companyId(summary.companyId())
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
                // Extended fields
                .itemDate(item.getItemDate())
                .vehiclePlate(item.getVehiclePlate())
                .orderNumber(item.getOrderNumber())
                .zone(item.getZone())
                .gasPercentage(item.getGasPercentage())
                .subtotal(item.calculateSubtotal())
                .total(item.calculateTotal())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
