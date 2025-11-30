package com.invoices.onboarding.domain.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "onboarding_progress")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "status")
    private String status; // REGISTERED, IN_PROGRESS, COMPLETED, SKIPPED

    @Column(name = "company_setup_completed")
    private boolean companySetupCompleted;

    @Column(name = "first_client_created")
    private boolean firstClientCreated;

    @Column(name = "first_invoice_created")
    private boolean firstInvoiceCreated;

    @Column(name = "tour_completed")
    private boolean tourCompleted;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "skipped_at")
    private LocalDateTime skippedAt;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
