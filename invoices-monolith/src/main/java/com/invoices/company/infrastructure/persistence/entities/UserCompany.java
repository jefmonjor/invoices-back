package com.invoices.company.infrastructure.persistence.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "user_companies")
public class UserCompany {

    @EmbeddedId
    private UserCompanyId id;

    @Column(name = "role", nullable = false)
    private String role;

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
}
