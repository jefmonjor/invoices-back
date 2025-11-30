package com.invoices.verifactu.domain.ports;

public interface VerifactuPort {
    void sendInvoice(Long companyId, Long invoiceId);
}
