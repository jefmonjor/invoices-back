package com.invoices.invoice.infrastructure.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

/**
 * Service to load and validate Spanish tax rates from configuration
 * Loads IVA, Recargo de Equivalencia, and IRPF rates from tax_rates_2025.json
 */
@Service
@Slf4j
public class TaxRatesService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Getter
    private Map<String, TaxRate> ivaRates = new HashMap<>();

    @Getter
    private Map<BigDecimal, BigDecimal> recargoEquivalencia = new HashMap<>();

    @Getter
    private Map<String, TaxRate> irpfRates = new HashMap<>();

    @PostConstruct
    public void loadTaxRates() {
        try {
            ClassPathResource resource = new ClassPathResource("config/tax_rates_2025.json");
            try (var inputStream = resource.getInputStream()) {
                JsonNode root = objectMapper.readTree(inputStream);

                // Load IVA rates
                JsonNode ivaNode = root.get("iva_rates");
                if (ivaNode != null && ivaNode.isArray()) {
                    for (JsonNode node : ivaNode) {
                        String code = node.get("code").asText();
                        BigDecimal rate = BigDecimal.valueOf(node.get("rate").asDouble());
                        String name = node.get("name").asText();

                        ivaRates.put(code, new TaxRate(code, rate, name));
                        log.info("Loaded IVA rate: {} - {}%", name, rate);
                    }
                }

                // Load Recargo de Equivalencia
                JsonNode reNode = root.get("recargo_equivalencia");
                if (reNode != null && reNode.isArray()) {
                    for (JsonNode node : reNode) {
                        BigDecimal ivaRate = BigDecimal.valueOf(node.get("for_iva_rate").asDouble());
                        BigDecimal reRate = BigDecimal.valueOf(node.get("re_rate").asDouble());

                        recargoEquivalencia.put(ivaRate, reRate);
                        log.info("Loaded RE: IVA {}% -> RE {}%", ivaRate, reRate);
                    }
                }

                // Load IRPF rates
                JsonNode irpfNode = root.get("irpf_withholding_common");
                if (irpfNode != null && irpfNode.isArray()) {
                    for (JsonNode node : irpfNode) {
                        String code = node.get("code").asText();
                        BigDecimal rate = BigDecimal.valueOf(node.get("rate").asDouble());
                        String name = node.get("name").asText();

                        irpfRates.put(code, new TaxRate(code, rate, name));
                        log.info("Loaded IRPF rate: {} - {}%", name, rate);
                    }
                }

                log.info("Tax rates loaded successfully from tax_rates_2025.json");
            }
        } catch (IOException e) {
            log.error("Error loading tax rates configuration", e);
            // Load default values as fallback
            loadDefaultRates();
        }
    }

    private void loadDefaultRates() {
        log.warn("Loading default tax rates as fallback");

        // Default IVA rates
        ivaRates.put("IVA_GENERAL", new TaxRate("IVA_GENERAL", BigDecimal.valueOf(21.0), "General"));
        ivaRates.put("IVA_REDUCIDO", new TaxRate("IVA_REDUCIDO", BigDecimal.valueOf(10.0), "Reducido"));
        ivaRates.put("IVA_SUPERREDUCIDO", new TaxRate("IVA_SUPERREDUCIDO", BigDecimal.valueOf(4.0), "Superreducido"));

        // Default RE
        recargoEquivalencia.put(BigDecimal.valueOf(21.0), BigDecimal.valueOf(5.2));
        recargoEquivalencia.put(BigDecimal.valueOf(10.0), BigDecimal.valueOf(1.4));
        recargoEquivalencia.put(BigDecimal.valueOf(4.0), BigDecimal.valueOf(0.5));

        // Default IRPF
        irpfRates.put("IRPF_PROFESIONALES_STANDARD",
                new TaxRate("IRPF_PROFESIONALES_STANDARD", BigDecimal.valueOf(15.0), "Retención estándar"));
        irpfRates.put("IRPF_PROFESIONALES_REDUCED_FIRST_YEARS",
                new TaxRate("IRPF_PROFESIONALES_REDUCED_FIRST_YEARS", BigDecimal.valueOf(7.0), "Retención reducida"));
    }

    /**
     * Validate if the given VAT percentage is valid according to official rates
     */
    public boolean isValidIVA(BigDecimal vatPercentage) {
        return ivaRates.values().stream()
                .anyMatch(rate -> rate.getRate().compareTo(vatPercentage) == 0);
    }

    /**
     * Get Recargo de Equivalencia rate for a given IVA rate
     */
    public Optional<BigDecimal> getRecargoEquivalencia(BigDecimal ivaRate) {
        return Optional.ofNullable(recargoEquivalencia.get(ivaRate));
    }

    /**
     * Get all valid IVA rates
     */
    public List<BigDecimal> getValidIVARates() {
        return ivaRates.values().stream()
                .map(TaxRate::getRate)
                .sorted()
                .toList();
    }

    /**
     * Get IVA rate by code
     */
    public Optional<TaxRate> getIVAByCode(String code) {
        return Optional.ofNullable(ivaRates.get(code));
    }

    /**
     * Get IRPF rate by code
     */
    public Optional<TaxRate> getIRPFByCode(String code) {
        return Optional.ofNullable(irpfRates.get(code));
    }

    @Getter
    public static class TaxRate {
        private final String code;
        private final BigDecimal rate;
        private final String name;

        public TaxRate(String code, BigDecimal rate, String name) {
            this.code = code;
            this.rate = rate;
            this.name = name;
        }
    }
}
