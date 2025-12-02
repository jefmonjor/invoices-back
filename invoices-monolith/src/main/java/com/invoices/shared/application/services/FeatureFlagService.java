package com.invoices.shared.application.services;

import com.invoices.shared.domain.entities.FeatureFlag;
import com.invoices.shared.domain.ports.FeatureFlagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeatureFlagService {

    private final FeatureFlagRepository repository;

    @Cacheable(value = "featureFlags", key = "#featureName", unless = "#result == false")
    @Transactional(readOnly = true)
    public boolean isEnabled(String featureName, Long companyId) {
        Optional<FeatureFlag> flagOpt = repository.findByFeatureName(featureName);

        if (flagOpt.isEmpty()) {
            log.warn("Feature flag {} not found, defaulting to false", featureName);
            return false;
        }

        FeatureFlag flag = flagOpt.get();

        if (!flag.isEnabled()) {
            return false;
        }

        // Check whitelist
        if (flag.getWhitelist() != null && companyId != null && flag.getWhitelist().contains(companyId)) {
            return true;
        }

        // Check rollout percentage
        if (companyId != null) {
            int hash = Math.abs(companyId.hashCode() % 100);
            return hash < flag.getRolloutPercentage();
        }

        return flag.getRolloutPercentage() == 100;
    }

    // Helper for global features without company context
    public boolean isEnabled(String featureName) {
        return isEnabled(featureName, null);
    }
}
