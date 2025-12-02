package com.invoices.onboarding.application.services;

import com.invoices.onboarding.domain.entities.DemoTemplate;
import com.invoices.onboarding.domain.entities.OnboardingProgress;
import com.invoices.onboarding.domain.ports.DemoTemplateRepository;
import com.invoices.onboarding.domain.ports.OnboardingProgressRepository;
import com.invoices.onboarding.presentation.dto.OnboardingProgressDTO;
import com.invoices.security.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OnboardingService {

    private final OnboardingProgressRepository progressRepository;
    private final DemoTemplateRepository demoTemplateRepository;

    @Transactional(readOnly = true)
    public OnboardingProgressDTO getProgress() {
        Long companyId = SecurityUtils.getCurrentCompanyId();
        return progressRepository.findByCompanyId(companyId)
                .map(OnboardingProgressDTO::fromEntity)
                .orElseGet(() -> OnboardingProgressDTO.builder()
                        .status("REGISTERED")
                        .build());
    }

    @Transactional
    public void updateStep(String step, boolean completed) {
        Long companyId = SecurityUtils.getCurrentCompanyId();
        OnboardingProgress progress = progressRepository.findByCompanyId(companyId)
                .orElseGet(() -> OnboardingProgress.builder()
                        .companyId(companyId)
                        .status("IN_PROGRESS")
                        .build());

        switch (step) {
            case "COMPANY_SETUP":
                progress.setCompanySetupCompleted(completed);
                break;
            case "FIRST_CLIENT":
                progress.setFirstClientCreated(completed);
                break;
            case "FIRST_INVOICE":
                progress.setFirstInvoiceCreated(completed);
                break;
            case "TOUR":
                progress.setTourCompleted(completed);
                break;
            default:
                log.warn("Unknown onboarding step: {}", step);
                return;
        }

        checkCompletion(progress);
        progressRepository.save(progress);
    }

    @Transactional
    public void skipOnboarding() {
        Long companyId = SecurityUtils.getCurrentCompanyId();
        OnboardingProgress progress = progressRepository.findByCompanyId(companyId)
                .orElseGet(() -> OnboardingProgress.builder()
                        .companyId(companyId)
                        .build());

        progress.setStatus("SKIPPED");
        progress.setSkippedAt(LocalDateTime.now());
        progressRepository.save(progress);
    }

    @Transactional(readOnly = true)
    public List<String> getDemoData(String type) {
        // Assuming locale is 'es' for now, can be extracted from context
        return demoTemplateRepository.findByTemplateTypeAndLocale(type, "es").stream()
                .map(DemoTemplate::getTemplateData)
                .collect(Collectors.toList());
    }

    private void checkCompletion(OnboardingProgress progress) {
        if (progress.isCompanySetupCompleted() &&
                progress.isFirstClientCreated() &&
                progress.isFirstInvoiceCreated() &&
                progress.isTourCompleted()) {
            progress.setStatus("COMPLETED");
            progress.setCompletedAt(LocalDateTime.now());
        } else {
            progress.setStatus("IN_PROGRESS");
        }
    }
}
