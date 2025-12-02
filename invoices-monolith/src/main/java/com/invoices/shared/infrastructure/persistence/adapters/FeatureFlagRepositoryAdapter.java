package com.invoices.shared.infrastructure.persistence.adapters;

import com.invoices.shared.domain.entities.FeatureFlag;
import com.invoices.shared.domain.ports.FeatureFlagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class FeatureFlagRepositoryAdapter implements FeatureFlagRepository {

    private final com.invoices.shared.infrastructure.persistence.FeatureFlagRepository jpaRepository;

    @Override
    public Optional<FeatureFlag> findByFeatureName(String featureName) {
        return jpaRepository.findByFeatureName(featureName);
    }
}
