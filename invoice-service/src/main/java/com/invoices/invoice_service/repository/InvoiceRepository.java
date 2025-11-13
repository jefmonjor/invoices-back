package com.invoices.invoice_service.repository;

import com.invoices.invoice_service.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    /**
     * Encuentra una factura por su número
     */
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    /**
     * Encuentra todas las facturas de un cliente
     */
    List<Invoice> findByClientId(Long clientId);

    /**
     * Cuenta facturas cuyo número empiece con un prefijo específico
     * Útil para generar números de factura secuenciales
     */
    long countByInvoiceNumberStartingWith(String prefix);

    /**
     * Verifica si existe una factura con el número dado
     */
    boolean existsByInvoiceNumber(String invoiceNumber);
}
