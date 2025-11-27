package com.invoices.audit.domain.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "platform_admin_audit_logs")
@Getter
@Setter
@NoArgsConstructor
public class PlatformAdminAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_email", nullable = false)
    private String adminEmail;

    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "target_type", nullable = false)
    private String targetType; // e.g., "COMPANY", "USER"

    @Column(name = "target_id")
    private String targetId;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "ip_address")
    private String ipAddress;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public PlatformAdminAuditLog(String adminEmail, String action, String targetType, String targetId, String details) {
        this.adminEmail = adminEmail;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.details = details;
    }
}
