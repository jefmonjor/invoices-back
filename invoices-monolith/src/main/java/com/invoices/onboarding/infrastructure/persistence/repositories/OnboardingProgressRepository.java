package com.invoices.onboarding.infrastructure.persistence.repositories;

import com.invoices.onboarding.domain.entities.OnboardingProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OnboardingProgressRepository extends JpaRepository<OnboardingProgress, Long> {
    Optional<OnboardingProgress> findByCompanyId(Long companyId);
}
