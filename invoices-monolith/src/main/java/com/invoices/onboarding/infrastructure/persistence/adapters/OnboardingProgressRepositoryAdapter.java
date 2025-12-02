package com.invoices.onboarding.infrastructure.persistence.adapters;

import com.invoices.onboarding.domain.entities.OnboardingProgress;
import com.invoices.onboarding.domain.ports.OnboardingProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OnboardingProgressRepositoryAdapter implements OnboardingProgressRepository {

    private final com.invoices.onboarding.infrastructure.persistence.repositories.OnboardingProgressRepository jpaRepository;

    @Override
    public Optional<OnboardingProgress> findByCompanyId(Long companyId) {
        return jpaRepository.findByCompanyId(companyId);
    }

    @Override
    public OnboardingProgress save(OnboardingProgress progress) {
        return jpaRepository.save(progress);
    }
}
