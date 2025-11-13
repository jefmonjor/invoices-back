package com.invoices.invoice_service.service;

import com.invoices.invoice_service.client.DocumentResponse;
import com.invoices.invoice_service.client.DocumentServiceClient;
import com.invoices.invoice_service.client.UserDTO;
import com.invoices.invoice_service.client.UserServiceClient;
import com.invoices.invoice_service.dto.*;
import com.invoices.invoice_service.entity.Invoice;
import com.invoices.invoice_service.entity.InvoiceItem;
import com.invoices.invoice_service.entity.InvoiceStatus;
import com.invoices.invoice_service.exception.ClientNotFoundException;
import com.invoices.invoice_service.exception.InvoiceNotFoundException;
import com.invoices.invoice_service.kafka.InvoiceEvent;
import com.invoices.invoice_service.kafka.InvoiceEventProducer;
import com.invoices.invoice_service.repository.InvoiceRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio principal para gestionar facturas
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final UserServiceClient userServiceClient;
    private final DocumentServiceClient documentServiceClient;
    private final InvoiceEventProducer eventProducer;
    private final PdfGenerationService pdfGenerationService;

    /**
     * Crea una nueva factura
     */
    public InvoiceDTO createInvoice(CreateInvoiceRequest request) {
        log.info("Creando nueva factura para cliente: {}", request.getClientId());

        // Validar que el cliente existe
        validateClientExists(request.getClientId());

        // Generar número de factura automático
        String invoiceNumber = generateInvoiceNumber();
        log.debug("Número de factura generado: {}", invoiceNumber);

        // Crear la factura
        Invoice invoice = Invoice.builder()
                .invoiceNumber(invoiceNumber)
                .clientId(request.getClientId())
                .clientEmail(request.getClientEmail())
                .invoiceDate(request.getInvoiceDate())
                .dueDate(request.getDueDate())
                .status(InvoiceStatus.PENDING)
                .notes(request.getNotes())
                .build();

        // Agregar items y calcular totales
        for (CreateInvoiceItemRequest itemRequest : request.getItems()) {
            InvoiceItem item = InvoiceItem.builder()
                    .description(itemRequest.getDescription())
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(itemRequest.getUnitPrice())
                    .build();
            item.setTotal(item.calculateTotal());
            invoice.addItem(item);
        }

        // Calcular subtotal, tax y total
        BigDecimal subtotal = invoice.calculateSubtotal();
        BigDecimal tax = subtotal.multiply(new BigDecimal("0.19")); // 19% de IVA
        BigDecimal total = subtotal.add(tax);

        invoice.setSubtotal(subtotal);
        invoice.setTax(tax);
        invoice.setTotal(total);

        // Guardar en base de datos
        Invoice savedInvoice = invoiceRepository.save(invoice);
        log.info("Factura creada exitosamente: {}", savedInvoice.getInvoiceNumber());

        // Enviar evento Kafka
        sendInvoiceCreatedEvent(savedInvoice);

        return mapToDTO(savedInvoice);
    }

    /**
     * Obtiene una factura por ID
     */
    @Transactional(readOnly = true)
    public InvoiceDTO getInvoiceById(Long id) {
        log.debug("Buscando factura con ID: {}", id);
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new InvoiceNotFoundException(id));
        return mapToDTO(invoice);
    }

    /**
     * Obtiene todas las facturas
     */
    @Transactional(readOnly = true)
    public List<InvoiceDTO> getAllInvoices() {
        log.debug("Obteniendo todas las facturas");
        return invoiceRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene todas las facturas de un cliente específico
     */
    @Transactional(readOnly = true)
    public List<InvoiceDTO> getInvoicesByClientId(Long clientId) {
        log.debug("Obteniendo facturas del cliente: {}", clientId);
        return invoiceRepository.findByClientId(clientId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Actualiza una factura existente
     */
    public InvoiceDTO updateInvoice(Long id, UpdateInvoiceRequest request) {
        log.info("Actualizando factura con ID: {}", id);

        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new InvoiceNotFoundException(id));

        // Actualizar campos si están presentes
        if (request.getDueDate() != null) {
            invoice.setDueDate(request.getDueDate());
        }
        if (request.getStatus() != null) {
            invoice.setStatus(request.getStatus());
        }
        if (request.getNotes() != null) {
            invoice.setNotes(request.getNotes());
        }

        Invoice updatedInvoice = invoiceRepository.save(invoice);
        log.info("Factura actualizada exitosamente: {}", updatedInvoice.getInvoiceNumber());

        // Enviar evento Kafka
        sendInvoiceUpdatedEvent(updatedInvoice);

        return mapToDTO(updatedInvoice);
    }

    /**
     * Elimina una factura
     */
    public void deleteInvoice(Long id) {
        log.info("Eliminando factura con ID: {}", id);

        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new InvoiceNotFoundException(id));

        invoiceRepository.delete(invoice);
        log.info("Factura eliminada exitosamente: {}", invoice.getInvoiceNumber());
    }

    /**
     * Marca una factura como pagada
     */
    public InvoiceDTO markAsPaid(Long id) {
        log.info("Marcando factura como pagada, ID: {}", id);

        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new InvoiceNotFoundException(id));

        invoice.setStatus(InvoiceStatus.PAID);
        Invoice updatedInvoice = invoiceRepository.save(invoice);

        log.info("Factura marcada como pagada: {}", updatedInvoice.getInvoiceNumber());

        // Enviar evento Kafka
        sendInvoicePaidEvent(updatedInvoice);

        return mapToDTO(updatedInvoice);
    }

    /**
     * Genera un PDF de la factura y lo sube al document-service
     */
    public GeneratePdfResponse generatePdf(Long invoiceId) {
        log.info("Generando PDF para factura con ID: {}", invoiceId);

        // Buscar la factura
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));

        // Generar PDF
        byte[] pdfBytes = pdfGenerationService.generateInvoicePdf(invoice);

        // Crear MultipartFile para enviar al document-service
        String fileName = invoice.getInvoiceNumber() + ".pdf";
        MultipartFile pdfFile = new MockMultipartFile(
                "file",
                fileName,
                "application/pdf",
                pdfBytes
        );

        // Subir al document-service
        try {
            DocumentResponse documentResponse = documentServiceClient.uploadPdf(
                    pdfFile,
                    invoice.getInvoiceNumber()
            );

            log.info("PDF generado y subido exitosamente para factura: {}", invoice.getInvoiceNumber());

            return GeneratePdfResponse.builder()
                    .invoiceId(invoice.getId())
                    .documentId(documentResponse.getId())
                    .downloadUrl(documentResponse.getDownloadUrl())
                    .generatedAt(LocalDateTime.now())
                    .build();

        } catch (FeignException e) {
            log.error("Error al subir PDF al document-service para factura: {}", invoice.getInvoiceNumber(), e);
            throw new RuntimeException("Error al subir el PDF al servicio de documentos", e);
        }
    }

    /**
     * Genera un número de factura automático en formato INV-YYYY-NNNN
     */
    private String generateInvoiceNumber() {
        int year = LocalDate.now().getYear();
        String prefix = "INV-" + year + "-";
        long count = invoiceRepository.countByInvoiceNumberStartingWith(prefix);
        return String.format("INV-%d-%04d", year, count + 1);
    }

    /**
     * Valida que un cliente existe llamando al user-service
     */
    private void validateClientExists(Long clientId) {
        try {
            log.debug("Validando existencia del cliente: {}", clientId);
            UserDTO user = userServiceClient.getUserById(clientId);
            log.debug("Cliente validado: {}", user.getEmail());
        } catch (FeignException.NotFound e) {
            log.error("Cliente no encontrado con ID: {}", clientId);
            throw new ClientNotFoundException(clientId);
        } catch (FeignException e) {
            log.error("Error al comunicarse con user-service", e);
            throw new RuntimeException("Error al validar el cliente", e);
        }
    }

    /**
     * Envía un evento de factura creada
     */
    private void sendInvoiceCreatedEvent(Invoice invoice) {
        InvoiceEvent event = createInvoiceEvent("CREATED", invoice);
        eventProducer.sendInvoiceCreated(event);
    }

    /**
     * Envía un evento de factura actualizada
     */
    private void sendInvoiceUpdatedEvent(Invoice invoice) {
        InvoiceEvent event = createInvoiceEvent("UPDATED", invoice);
        eventProducer.sendInvoiceUpdated(event);
    }

    /**
     * Envía un evento de factura pagada
     */
    private void sendInvoicePaidEvent(Invoice invoice) {
        InvoiceEvent event = createInvoiceEvent("PAID", invoice);
        eventProducer.sendInvoicePaid(event);
    }

    /**
     * Crea un evento de factura
     */
    private InvoiceEvent createInvoiceEvent(String eventType, Invoice invoice) {
        return new InvoiceEvent(
                eventType,
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getClientId(),
                invoice.getClientEmail(),
                invoice.getTotal(),
                invoice.getStatus().toString(),
                LocalDateTime.now()
        );
    }

    /**
     * Mapea una entidad Invoice a InvoiceDTO
     */
    private InvoiceDTO mapToDTO(Invoice invoice) {
        List<InvoiceItemDTO> itemDTOs = invoice.getItems().stream()
                .map(this::mapItemToDTO)
                .collect(Collectors.toList());

        return InvoiceDTO.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .clientId(invoice.getClientId())
                .clientEmail(invoice.getClientEmail())
                .invoiceDate(invoice.getInvoiceDate())
                .dueDate(invoice.getDueDate())
                .subtotal(invoice.getSubtotal())
                .tax(invoice.getTax())
                .total(invoice.getTotal())
                .status(invoice.getStatus())
                .notes(invoice.getNotes())
                .items(itemDTOs)
                .createdAt(invoice.getCreatedAt())
                .build();
    }

    /**
     * Mapea una entidad InvoiceItem a InvoiceItemDTO
     */
    private InvoiceItemDTO mapItemToDTO(InvoiceItem item) {
        return InvoiceItemDTO.builder()
                .id(item.getId())
                .description(item.getDescription())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .total(item.getTotal())
                .build();
    }
}
