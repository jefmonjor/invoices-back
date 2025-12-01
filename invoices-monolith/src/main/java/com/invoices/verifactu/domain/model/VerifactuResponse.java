package com.invoices.verifactu.domain.model;

import lombok.Data;

/**
 * Parsed Veri*Factu response with extracted data.
 */
@Data
public class VerifactuResponse {
    private boolean success;
    private String responseCode;
    private String message;

    // Success fields
    private String csv; // Código Seguro de Verificación
    private String qrData; // QR code data (URL)

    // Error fields
    private String errorCode;
    private String errorMessage;
}
