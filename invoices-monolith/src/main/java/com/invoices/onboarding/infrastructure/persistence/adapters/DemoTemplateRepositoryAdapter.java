package com.invoices.onboarding.infrastructure.persistence.adapters;

import com.invoices.onboarding.domain.entities.DemoTemplate;
import com.invoices.onboarding.domain.ports.DemoTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class DemoTemplateRepositoryAdapter implements DemoTemplateRepository {

    private final com.invoices.onboarding.infrastructure.persistence.repositories.DemoTemplateRepository jpaRepository;

    @Override
    public List<DemoTemplate> findByTemplateTypeAndLocale(String type, String locale) {
        return jpaRepository.findByTemplateTypeAndLocale(type, locale);
    }
}
