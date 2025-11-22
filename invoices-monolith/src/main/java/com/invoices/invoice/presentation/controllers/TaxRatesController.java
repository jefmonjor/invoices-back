package com.invoices.invoice.presentation.controllers;

import com.invoices.invoice.infrastructure.config.TaxRatesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller to expose Spanish tax rates from tax_rates_2025.json
 * Provides valid IVA, Recargo de Equivalencia, and IRPF rates for frontend
 */
@RestController
@RequestMapping("/api/tax-rates")
@RequiredArgsConstructor
public class TaxRatesController {

    private final TaxRatesService taxRatesService;

    /**
     * GET /api/tax-rates - Get all available tax rates
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getTaxRates() {
        Map<String, Object> response = new HashMap<>();

        // IVA rates
        Map<String, Object> ivaRates = new HashMap<>();
        taxRatesService.getIvaRates().forEach((code, rate) -> {
            Map<String, Object> rateInfo = new HashMap<>();
            rateInfo.put("rate", rate.getRate());
            rateInfo.put("name", rate.getName());
            ivaRates.put(code, rateInfo);
        });
        response.put("iva_rates", ivaRates);

        // Recargo de Equivalencia
        Map<String, BigDecimal> reRates = new HashMap<>();
        taxRatesService.getRecargoEquivalencia().forEach((ivaRate, reRate) -> {
            reRates.put(ivaRate.toString(), reRate);
        });
        response.put("recargo_equivalencia", reRates);

        // IRPF rates
        Map<String, Object> irpfRates = new HashMap<>();
        taxRatesService.getIrpfRates().forEach((code, rate) -> {
            Map<String, Object> rateInfo = new HashMap<>();
            rateInfo.put("rate", rate.getRate());
            rateInfo.put("name", rate.getName());
            irpfRates.put(code, rateInfo);
        });
        response.put("irpf_rates", irpfRates);

        // Valid IVA rates list (for dropdown)
        response.put("valid_iva_rates", taxRatesService.getValidIVARates());

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/tax-rates/iva/validate - Validate if IVA rate is official
     */
    @GetMapping("/iva/validate")
    public ResponseEntity<Map<String, Object>> validateIVA(@RequestParam BigDecimal rate) {
        boolean isValid = taxRatesService.isValidIVA(rate);

        Map<String, Object> response = new HashMap<>();
        response.put("rate", rate);
        response.put("valid", isValid);

        if (!isValid) {
            response.put("message", "Tipo de IVA no válido. Tipos oficiales: " + taxRatesService.getValidIVARates());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/tax-rates/recargo - Get RE rate for IVA rate
     */
    @GetMapping("/recargo")
    public ResponseEntity<Map<String, Object>> getRecargoForIVA(@RequestParam BigDecimal ivaRate) {
        Map<String, Object> response = new HashMap<>();
        response.put("iva_rate", ivaRate);

        taxRatesService.getRecargoEquivalencia(ivaRate).ifPresentOrElse(
                reRate -> {
                    response.put("re_rate", reRate);
                    response.put("found", true);
                },
                () -> {
                    response.put("found", false);
                    response.put("message", "No hay recargo de equivalencia definido para IVA " + ivaRate + "%");
                });

        return ResponseEntity.ok(response);
    }
}
