package com.invoices.verifactu.domain.model;

/**
 * Operating mode for Veri*Factu system.
 */
public enum VerifactuMode {
    /**
     * Sandbox mode for testing with AEAT test endpoints.
     */
    SANDBOX,

    /**
     * Production mode for live AEAT submissions.
     */
    PRODUCTION
}
