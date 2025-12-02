package com.invoices.company.domain.entities;

import java.time.LocalDateTime;

/**
 * Domain entity representing a company invitation.
 * Encapsulates information about invitations to join a company.
 */
public class CompanyInvitation {

    private Long id;
    private Long companyId;
    private String email;
    private String token;
    private String role;
    private String status;  // PENDING, ACCEPTED, EXPIRED
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;

    public CompanyInvitation() {
    }

    public CompanyInvitation(Long companyId, String email, String token, String role, LocalDateTime expiresAt) {
        this.companyId = companyId;
        this.email = email;
        this.token = token;
        this.role = role;
        this.expiresAt = expiresAt;
        this.status = "PENDING";
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "CompanyInvitation{" +
                "id=" + id +
                ", companyId=" + companyId +
                ", email='" + email + '\'' +
                ", token='" + token + '\'' +
                ", role='" + role + '\'' +
                ", status='" + status + '\'' +
                ", expiresAt=" + expiresAt +
                ", createdAt=" + createdAt +
                '}';
    }
}
