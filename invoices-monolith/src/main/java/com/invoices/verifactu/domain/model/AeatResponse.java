package com.invoices.verifactu.domain.model;

import lombok.Data;

/**
 * Raw response from AEAT SOAP service.
 */
@Data
public class AeatResponse {
    private boolean success;
    private String code;
    private String message;
    private String csv; // Código Seguro de Verificación
    private String rawXml; // Full SOAP response for debugging
}
