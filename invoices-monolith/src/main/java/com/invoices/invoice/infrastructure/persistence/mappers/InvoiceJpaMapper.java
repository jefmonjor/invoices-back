package com.invoices.invoice.infrastructure.persistence.mappers;

import com.invoices.invoice.domain.entities.Invoice;
import com.invoices.invoice.domain.entities.InvoiceItem;
import com.invoices.invoice.domain.entities.InvoiceStatus;
import com.invoices.invoice.infrastructure.persistence.entities.InvoiceItemJpaEntity;
import com.invoices.invoice.infrastructure.persistence.entities.InvoiceJpaEntity;
import com.invoices.user.domain.ports.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper between domain Invoice and JPA InvoiceJpaEntity.
 * Keeps domain and infrastructure layers decoupled.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InvoiceJpaMapper {

    private final UserRepository userRepository;

    /**
     * Gets the current authenticated user's ID from SecurityContext.
     * Falls back to user ID 1 (admin) if no authentication is present.
     *
     * @return the current user's ID
     */
    private Long getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                log.warn("No authentication found in SecurityContext, defaulting to user ID 1");
                return 1L;
            }

            String email = authentication.getName();
            return userRepository.findByEmail(email)
                    .map(user -> user.getId())
                    .orElseGet(() -> {
                        log.warn("User not found for email: {}, defaulting to user ID 1", email);
                        return 1L;
                    });
        } catch (Exception e) {
            log.error("Error getting current user ID from SecurityContext", e);
            return 1L;
        }
    }

    public InvoiceJpaEntity toJpaEntity(Invoice invoice) {
        if (invoice == null) {
            return null;
        }

        InvoiceJpaEntity jpaEntity = new InvoiceJpaEntity();
        jpaEntity.setId(invoice.getId());
        jpaEntity.setUserId(getCurrentUserId());
        jpaEntity.setCompanyId(invoice.getCompanyId());
        jpaEntity.setClientId(invoice.getClientId());
        jpaEntity.setInvoiceNumber(invoice.getInvoiceNumber());
        jpaEntity.setSettlementNumber(invoice.getSettlementNumber());
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

        // VERI*FACTU fields
        jpaEntity.setDocumentJson(invoice.getDocumentJson());
        jpaEntity.setCanonicalJson(invoice.getCanonicalJson());
        jpaEntity.setDocumentHash(invoice.getDocumentHash());
        jpaEntity.setPreviousDocumentHash(invoice.getPreviousDocumentHash());
        jpaEntity.setVerifactuStatus(invoice.getVerifactuStatus());
        jpaEntity.setVerifactuTxId(invoice.getVerifactuTxId());
        jpaEntity.setVerifactuRawResponse(invoice.getVerifactuRawResponse());
        jpaEntity.setVerifactuError(invoice.getVerifactuError());
        jpaEntity.setPdfServerPath(invoice.getPdfServerPath());
        jpaEntity.setPdfIsFinal(invoice.getPdfIsFinal());
        jpaEntity.setPdfIsFinal(invoice.getPdfIsFinal());
        jpaEntity.setQrPayload(invoice.getQrPayload());
        jpaEntity.setHash(invoice.getHash());
        jpaEntity.setLastHashBefore(invoice.getLastHashBefore());
        jpaEntity.setCsvAcuse(invoice.getCsvAcuse());
        jpaEntity.setQrData(invoice.getQrData());
        jpaEntity.setXmlContent(invoice.getXmlContent());
        jpaEntity.setRectificativa(invoice.isRectificativa());
        jpaEntity.setRectifiesInvoiceId(invoice.getRectifiesInvoiceId());
        jpaEntity.setVerifactuRetryCount(invoice.getVerifactuRetryCount());

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
                jpaEntity.getRePercentage());

        // Set status from database without validation (using internal method)
        if (jpaEntity.getStatus() != null) {
            invoice.setStatusInternal(InvoiceStatus.valueOf(jpaEntity.getStatus()));
        }

        // Set timestamps from database FIRST (before setting notes)
        if (jpaEntity.getCreatedAt() != null && jpaEntity.getUpdatedAt() != null) {
            invoice.setTimestampsInternal(jpaEntity.getCreatedAt(), jpaEntity.getUpdatedAt());
        }

        // Set notes if present (using internal method to avoid updating timestamp)
        if (jpaEntity.getNotes() != null) {
            invoice.setNotesInternal(jpaEntity.getNotes());
        }

        // Set settlement number if present (using internal method to avoid updating
        // timestamp)
        if (jpaEntity.getSettlementNumber() != null) {
            invoice.setSettlementNumberInternal(jpaEntity.getSettlementNumber());
        }

        // Add items without state validation (using internal method)
        // This is necessary to reconstruct invoices in FINALIZED or PAID status
        jpaEntity.getItems().forEach(itemJpa -> {
            InvoiceItem item = toDomainItemEntity(itemJpa);
            invoice.addItemInternal(item);
        });

        // Set VERI*FACTU fields if present
        if (jpaEntity.getDocumentJson() != null) {
            invoice.setDocumentJson(jpaEntity.getDocumentJson());
        }
        if (jpaEntity.getCanonicalJson() != null) {
            invoice.setCanonicalJson(jpaEntity.getCanonicalJson());
        }
        if (jpaEntity.getDocumentHash() != null) {
            invoice.setDocumentHash(jpaEntity.getDocumentHash());
        }
        if (jpaEntity.getPreviousDocumentHash() != null) {
            invoice.setPreviousDocumentHash(jpaEntity.getPreviousDocumentHash());
        }
        if (jpaEntity.getVerifactuStatus() != null) {
            invoice.setVerifactuStatus(jpaEntity.getVerifactuStatus());
        }
        if (jpaEntity.getVerifactuTxId() != null) {
            invoice.setVerifactuTxId(jpaEntity.getVerifactuTxId());
        }
        if (jpaEntity.getVerifactuRawResponse() != null) {
            invoice.setVerifactuRawResponse(jpaEntity.getVerifactuRawResponse());
        }
        if (jpaEntity.getPdfServerPath() != null) {
            invoice.setPdfServerPath(jpaEntity.getPdfServerPath());
        }
        if (jpaEntity.getPdfIsFinal() != null) {
            invoice.setPdfIsFinal(jpaEntity.getPdfIsFinal());
        }
        if (jpaEntity.getQrPayload() != null) {
            invoice.setQrPayload(jpaEntity.getQrPayload());
        }
        if (jpaEntity.getHash() != null) {
            invoice.setHash(jpaEntity.getHash());
        }
        if (jpaEntity.getLastHashBefore() != null) {
            invoice.setLastHashBefore(jpaEntity.getLastHashBefore());
        }
        if (jpaEntity.getCsvAcuse() != null) {
            invoice.setCsvAcuse(jpaEntity.getCsvAcuse());
        }
        if (jpaEntity.getQrData() != null) {
            invoice.setQrData(jpaEntity.getQrData());
        }
        if (jpaEntity.getXmlContent() != null) {
            invoice.setXmlContent(jpaEntity.getXmlContent());
        }
        if (jpaEntity.getVerifactuError() != null) {
            invoice.setVerifactuError(jpaEntity.getVerifactuError());
        }
        invoice.setRectificativa(jpaEntity.isRectificativa());
        if (jpaEntity.getRectifiesInvoiceId() != null) {
            invoice.setRectifiesInvoiceId(jpaEntity.getRectifiesInvoiceId());
        }
        if (jpaEntity.getVerifactuRetryCount() != null) {
            invoice.setVerifactuRetryCount(jpaEntity.getVerifactuRetryCount());
        }

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
        // Extended fields
        jpaItem.setItemDate(item.getItemDate());
        jpaItem.setVehiclePlate(item.getVehiclePlate());
        jpaItem.setOrderNumber(item.getOrderNumber());
        jpaItem.setZone(item.getZone());
        jpaItem.setGasPercentage(item.getGasPercentage());
        jpaItem.setSubtotal(item.calculateSubtotal());
        jpaItem.setTotal(item.calculateTotal());
        jpaItem.setCreatedAt(item.getCreatedAt());
        jpaItem.setUpdatedAt(item.getUpdatedAt());
        return jpaItem;
    }

    private InvoiceItem toDomainItemEntity(InvoiceItemJpaEntity jpaItem) {
        InvoiceItem item = new InvoiceItem(
                jpaItem.getId(),
                jpaItem.getInvoice().getId(),
                jpaItem.getDescription(),
                jpaItem.getUnits(),
                jpaItem.getPrice(),
                jpaItem.getVatPercentage(),
                jpaItem.getDiscountPercentage() != null ? jpaItem.getDiscountPercentage() : java.math.BigDecimal.ZERO);

        // Set extended fields if present
        if (jpaItem.getItemDate() != null) {
            item.setItemDate(jpaItem.getItemDate());
        }
        if (jpaItem.getVehiclePlate() != null) {
            item.setVehiclePlate(jpaItem.getVehiclePlate());
        }
        if (jpaItem.getOrderNumber() != null) {
            item.setOrderNumber(jpaItem.getOrderNumber());
        }
        if (jpaItem.getZone() != null) {
            item.setZone(jpaItem.getZone());
        }
        if (jpaItem.getGasPercentage() != null) {
            item.setGasPercentage(jpaItem.getGasPercentage());
        }

        return item;
    }
}
