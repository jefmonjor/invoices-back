package com.invoices.onboarding.domain.ports;

import com.invoices.onboarding.domain.entities.OnboardingProgress;

import java.util.Optional;

/**
 * Port for accessing OnboardingProgress data.
 * This is a domain interface that will be implemented by infrastructure layer.
 * Keeps domain layer independent of database implementation.
 */
public interface OnboardingProgressRepository {

    /**
     * Find onboarding progress for a company.
     *
     * @param companyId the company ID
     * @return Optional containing the progress if found
     */
    Optional<OnboardingProgress> findByCompanyId(Long companyId);

    /**
     * Save onboarding progress.
     *
     * @param progress the progress to save
     * @return the saved progress
     */
    OnboardingProgress save(OnboardingProgress progress);
}
