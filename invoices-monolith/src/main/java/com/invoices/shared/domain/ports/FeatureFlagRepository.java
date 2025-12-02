package com.invoices.shared.domain.ports;

import com.invoices.shared.domain.entities.FeatureFlag;

import java.util.Optional;

/**
 * Port for accessing FeatureFlag data.
 * This is a domain interface that will be implemented by infrastructure layer.
 * Keeps domain layer independent of database implementation.
 */
public interface FeatureFlagRepository {

    /**
     * Find a feature flag by its name.
     *
     * @param featureName the name of the feature flag
     * @return Optional containing the feature flag if found
     */
    Optional<FeatureFlag> findByFeatureName(String featureName);
}
