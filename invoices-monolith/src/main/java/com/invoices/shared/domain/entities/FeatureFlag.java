package com.invoices.shared.domain.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "feature_flags")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String featureName;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private int rolloutPercentage; // 0-100

    @ElementCollection
    @CollectionTable(name = "feature_flag_whitelist", joinColumns = @JoinColumn(name = "feature_flag_id"))
    @Column(name = "company_id")
    private List<Long> whitelist;

    private Instant updatedAt;
    private String updatedBy;

    @PrePersist
    @PreUpdate
    public void prePersist() {
        this.updatedAt = Instant.now();
    }
}
