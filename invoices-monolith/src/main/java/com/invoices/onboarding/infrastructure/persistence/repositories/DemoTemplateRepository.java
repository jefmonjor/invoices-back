package com.invoices.onboarding.infrastructure.persistence.repositories;

import com.invoices.onboarding.domain.entities.DemoTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DemoTemplateRepository extends JpaRepository<DemoTemplate, Long> {
    List<DemoTemplate> findByTemplateTypeAndLocale(String templateType, String locale);
}
