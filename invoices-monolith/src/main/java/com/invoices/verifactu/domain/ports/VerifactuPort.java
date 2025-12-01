package com.invoices.verifactu.domain.ports;

public interface VerifactuPort {
    void sendInvoice(Long companyId, Long invoiceId);

    void sendInvoice(Long invoiceId);

    void processWebhook(String payload);
}
