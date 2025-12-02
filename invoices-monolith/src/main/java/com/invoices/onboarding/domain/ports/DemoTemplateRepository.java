package com.invoices.onboarding.domain.ports;

import com.invoices.onboarding.domain.entities.DemoTemplate;

import java.util.List;

/**
 * Port for accessing DemoTemplate data.
 * This is a domain interface that will be implemented by infrastructure layer.
 * Keeps domain layer independent of database implementation.
 */
public interface DemoTemplateRepository {

    /**
     * Find demo templates by type and locale.
     *
     * @param type the template type
     * @param locale the locale (e.g., 'es', 'en')
     * @return list of matching demo templates
     */
    List<DemoTemplate> findByTemplateTypeAndLocale(String type, String locale);
}
