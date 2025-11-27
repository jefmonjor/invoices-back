package com.invoices.invoice.infrastructure.verifactu;

/**
 * Interface for VeriFactu verification services.
 * Allows switching between real AEAT integration and mock implementation.
 */
public interface VerifactuServiceInterface {

    /**
     * Process VeriFactu verification for an invoice
     * 
     * @param invoiceId ID of the invoice to process
     */
    void processInvoice(Long invoiceId);

    /**
     * Get service type identifier
     * 
     * @return "REAL" or "MOCK"
     */
    String getServiceType();

    /**
     * Process VeriFactu webhook payload
     * 
     * @param payload JSON payload from AEAT
     */
    void processWebhook(String payload);
}
