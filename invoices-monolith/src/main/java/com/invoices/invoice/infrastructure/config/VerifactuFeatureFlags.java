package com.invoices.invoice.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.invoices.invoice.domain.entities.Invoice;

/**
 * Feature flags for VeriFactu rollout control
 * Allows gradual activation by percentage based on company tax ID hash
 */
@Configuration
@ConfigurationProperties(prefix = "verifactu")
@Getter
@Setter
@Slf4j
public class VerifactuFeatureFlags {

    /**
     * Global VeriFactu feature enablement
     */
    private boolean enabled = false;

    /**
     * Use mock service instead of real SOAP service
     */
    private boolean useMock = true;

    /**
     * Rollout percentage (0-100)
     * Controls what percentage of invoices should use real VeriFactu service
     */
    private RolloutConfig rollout = new RolloutConfig();

    @Getter
    @Setter
    public static class RolloutConfig {
        /**
         * Percentage of invoices to enable (0-100)
         * Based on hash of company tax ID
         */
        private int percentage = 0;
    }

    /**
     * Determines if VeriFactu should be enabled for this invoice
     * Uses consistent hashing based on company tax ID for rollout
     *
     * @param invoice Invoice to check
     * @return true if VeriFactu should be used for this invoice
     */
    public boolean shouldUseVeriFactu(Invoice invoice) {
        if (!enabled) {
            log.debug("VeriFactu globally disabled");
            return false;
        }

        if (rollout.getPercentage() >= 100) {
            log.debug("VeriFactu enabled for all invoices (100% rollout)");
            return true;
        }

        if (rollout.getPercentage() <= 0) {
            log.debug("VeriFactu disabled by rollout percentage (0%)");
            return false;
        }

        // Use consistent hashing based on company tax ID
        String taxId = invoice.getCompany() != null ? invoice.getCompany().getTaxId() : "";
        int hash = Math.abs(taxId.hashCode());
        boolean enabled = (hash % 100) < rollout.getPercentage();

        log.debug("VeriFactu {} for invoice {} (company: {}, rollout: {}%)",
                enabled ? "ENABLED" : "DISABLED",
                invoice.getId(),
                taxId,
                rollout.getPercentage());

        return enabled;
    }

    /**
     * Determines if real SOAP service should be used (vs mock)
     *
     * @return true if real service should be used
     */
    public boolean shouldUseRealService() {
        return enabled && !useMock;
    }

    /**
     * Check if VeriFactu is enabled for a specific company by tax ID
     *
     * @param taxId Company tax ID
     * @return true if enabled for this company
     */
    public boolean isEnabledForCompany(String taxId) {
        if (!enabled || rollout.getPercentage() <= 0) {
            return false;
        }

        if (rollout.getPercentage() >= 100) {
            return true;
        }

        int hash = Math.abs(taxId.hashCode());
        return (hash % 100) < rollout.getPercentage();
    }
}
