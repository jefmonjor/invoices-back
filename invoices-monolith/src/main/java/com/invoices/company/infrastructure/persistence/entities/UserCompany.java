package com.invoices.company.infrastructure.persistence.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "user_companies")
public class UserCompany {

    @EmbeddedId
    private UserCompanyId id;

    @Column(name = "role", nullable = false, length = 50)
    private String role;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private com.invoices.user.infrastructure.persistence.entities.UserJpaEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("companyId")
    @JoinColumn(name = "company_id")
    private com.invoices.invoice.infrastructure.persistence.entities.CompanyJpaEntity company;

    public UserCompany() {
    }

    public UserCompany(UserCompanyId id, String role) {
        this.id = id;
        this.role = role;
    }

    public UserCompanyId getId() {
        return id;
    }

    public void setId(UserCompanyId id) {
        this.id = id;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public com.invoices.user.infrastructure.persistence.entities.UserJpaEntity getUser() {
        return user;
    }

    public void setUser(com.invoices.user.infrastructure.persistence.entities.UserJpaEntity user) {
        this.user = user;
    }

    public com.invoices.invoice.infrastructure.persistence.entities.CompanyJpaEntity getCompany() {
        return company;
    }

    public void setCompany(com.invoices.invoice.infrastructure.persistence.entities.CompanyJpaEntity company) {
        this.company = company;
    }
}
