package com.invoices.invoice_service.service;

import com.invoices.invoice_service.dto.InvoiceConfigDTO;
import com.invoices.invoice_service.dto.InvoiceDTO;
import com.invoices.invoice_service.dto.InvoiceItemDTO;
import com.invoices.invoice_service.entity.Invoice;
import com.invoices.invoice_service.entity.InvoiceItem;
import com.invoices.invoice_service.mapper.InvoiceItemMapper;
import com.invoices.invoice_service.mapper.InvoiceMapper;
import com.invoices.invoice_service.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceMapper invoiceMapper;
    private final InvoiceItemMapper invoiceItemMapper;

    @Transactional
    public InvoiceDTO createInvoiceFromConfig(InvoiceConfigDTO configDTO) {
        // Validar que no exista una factura con el mismo número
        if (invoiceRepository.findByInvoiceNumber(configDTO.getInvoiceNumber()).isPresent()) {
            throw new IllegalArgumentException("Invoice number already exists: " + configDTO.getInvoiceNumber());
        }

        // Crear la factura
        Invoice invoice = Invoice.builder()
                .invoiceNumber(configDTO.getInvoiceNumber())
                .userId(configDTO.getUserId())
                .clientId(configDTO.getClientId())
                .issueDate(LocalDateTime.now())
                .status("Pendiente")
                .notes(configDTO.getNotes())
                .iban(configDTO.getIban())
                .build();

        // Si hay items, procesarlos y calcular totales
        if (configDTO.getItems() != null && !configDTO.getItems().isEmpty()) {
            List<InvoiceItem> items = configDTO.getItems().stream()
                    .map(itemDTO -> {
                        InvoiceItem item = invoiceItemMapper.toEntity(itemDTO);
                        item.setInvoice(invoice);
                        calculateItemTotals(item);
                        return item;
                    })
                    .collect(Collectors.toList());

            invoice.setItems(items);
            calculateInvoiceTotals(invoice);
        } else {
            // Si no hay items, usar el baseAmount del config
            invoice.setBaseAmount(configDTO.getBaseAmount() != null ? configDTO.getBaseAmount() : BigDecimal.ZERO);
            invoice.setIrpfPercentage(configDTO.getIrpfPercentage() != null ? configDTO.getIrpfPercentage() : BigDecimal.ZERO);
            invoice.setRePercentage(configDTO.getRePercentage() != null ? configDTO.getRePercentage() : BigDecimal.ZERO);
            calculateInvoiceTotalsManual(invoice);
        }

        invoice = invoiceRepository.save(invoice);
        return invoiceMapper.toDTO(invoice);
    }

    @Transactional(readOnly = true)
    public InvoiceDTO getInvoiceById(Integer id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found with id: " + id));
        return invoiceMapper.toDTO(invoice);
    }

    @Transactional(readOnly = true)
    public Optional<InvoiceDTO> findByInvoiceNumber(String invoiceNumber) {
        return invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .map(invoiceMapper::toDTO);
    }

    private void calculateItemTotals(InvoiceItem item) {
        // Calcular subtotal: units * price
        BigDecimal subtotal = item.getPrice()
                .multiply(new BigDecimal(item.getUnits()))
                .setScale(2, RoundingMode.HALF_UP);

        // Aplicar descuento si existe
        if (item.getDiscountPercentage() != null && item.getDiscountPercentage().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discountAmount = subtotal
                    .multiply(item.getDiscountPercentage())
                    .divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
            subtotal = subtotal.subtract(discountAmount);
        }

        item.setSubtotal(subtotal);

        // Calcular total: subtotal + IVA
        BigDecimal vatAmount = subtotal
                .multiply(item.getVatPercentage())
                .divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);

        BigDecimal total = subtotal.add(vatAmount);
        item.setTotal(total.setScale(2, RoundingMode.HALF_UP));
    }

    private void calculateInvoiceTotals(Invoice invoice) {
        if (invoice.getItems() == null || invoice.getItems().isEmpty()) {
            invoice.setBaseAmount(BigDecimal.ZERO);
            invoice.setVatAmount(BigDecimal.ZERO);
            invoice.setIrpfAmount(BigDecimal.ZERO);
            invoice.setReAmount(BigDecimal.ZERO);
            invoice.setTotalAmount(BigDecimal.ZERO);
            return;
        }

        // Calcular base imponible (suma de subtotales)
        BigDecimal baseAmount = invoice.getItems().stream()
                .map(InvoiceItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calcular VAT total (suma del IVA de todos los items)
        BigDecimal vatAmount = invoice.getItems().stream()
                .map(item -> item.getSubtotal()
                        .multiply(item.getVatPercentage())
                        .divide(new BigDecimal(100), 2, RoundingMode.HALF_UP))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Asumir que VAT es 21% si no está especificado
        if (invoice.getVatPercentage() == null) {
            invoice.setVatPercentage(new BigDecimal("21.00"));
        }

        invoice.setBaseAmount(baseAmount);
        invoice.setVatAmount(vatAmount);

        // Calcular IRPF e RE
        BigDecimal irpfPercentage = invoice.getIrpfPercentage() != null ? invoice.getIrpfPercentage() : BigDecimal.ZERO;
        BigDecimal rePercentage = invoice.getRePercentage() != null ? invoice.getRePercentage() : BigDecimal.ZERO;

        BigDecimal irpfAmount = baseAmount
                .multiply(irpfPercentage)
                .divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);

        BigDecimal reAmount = baseAmount
                .multiply(rePercentage)
                .divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);

        invoice.setIrpfAmount(irpfAmount);
        invoice.setReAmount(reAmount);

        // Total = base + VAT - IRPF + RE
        BigDecimal totalAmount = baseAmount
                .add(vatAmount)
                .subtract(irpfAmount)
                .add(reAmount)
                .setScale(2, RoundingMode.HALF_UP);

        invoice.setTotalAmount(totalAmount);
    }

    private void calculateInvoiceTotalsManual(Invoice invoice) {
        BigDecimal baseAmount = invoice.getBaseAmount() != null ? invoice.getBaseAmount() : BigDecimal.ZERO;
        BigDecimal irpfPercentage = invoice.getIrpfPercentage() != null ? invoice.getIrpfPercentage() : BigDecimal.ZERO;
        BigDecimal rePercentage = invoice.getRePercentage() != null ? invoice.getRePercentage() : BigDecimal.ZERO;

        // Asumir VAT 21% por defecto
        if (invoice.getVatPercentage() == null) {
            invoice.setVatPercentage(new BigDecimal("21.00"));
        }

        BigDecimal vatAmount = baseAmount
                .multiply(invoice.getVatPercentage())
                .divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);

        BigDecimal irpfAmount = baseAmount
                .multiply(irpfPercentage)
                .divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);

        BigDecimal reAmount = baseAmount
                .multiply(rePercentage)
                .divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);

        invoice.setVatAmount(vatAmount);
        invoice.setIrpfAmount(irpfAmount);
        invoice.setReAmount(reAmount);

        // Total = base + VAT - IRPF + RE
        BigDecimal totalAmount = baseAmount
                .add(vatAmount)
                .subtract(irpfAmount)
                .add(reAmount)
                .setScale(2, RoundingMode.HALF_UP);

        invoice.setTotalAmount(totalAmount);
    }
}
