package com.invoices.onboarding.presentation.dto;

import com.invoices.onboarding.domain.entities.OnboardingProgress;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingProgressDTO {
    private String status;
    private boolean companySetupCompleted;
    private boolean firstClientCreated;
    private boolean firstInvoiceCreated;
    private boolean tourCompleted;
    private boolean completed;
    private boolean skipped;

    public static OnboardingProgressDTO fromEntity(OnboardingProgress entity) {
        if (entity == null) {
            return OnboardingProgressDTO.builder()
                    .status("REGISTERED")
                    .companySetupCompleted(false)
                    .firstClientCreated(false)
                    .firstInvoiceCreated(false)
                    .tourCompleted(false)
                    .completed(false)
                    .skipped(false)
                    .build();
        }
        return OnboardingProgressDTO.builder()
                .status(entity.getStatus())
                .companySetupCompleted(entity.isCompanySetupCompleted())
                .firstClientCreated(entity.isFirstClientCreated())
                .firstInvoiceCreated(entity.isFirstInvoiceCreated())
                .tourCompleted(entity.isTourCompleted())
                .completed("COMPLETED".equals(entity.getStatus()))
                .skipped("SKIPPED".equals(entity.getStatus()))
                .build();
    }
}
