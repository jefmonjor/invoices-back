package com.invoices.invoice_service.repository;

import com.invoices.invoice_service.entity.InvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, Long> {

    /**
     * Encuentra todos los items de una factura específica
     */
    List<InvoiceItem> findByInvoiceId(Long invoiceId);

    /**
     * Elimina todos los items de una factura específica
     */
    void deleteByInvoiceId(Long invoiceId);
}
