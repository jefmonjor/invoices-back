package com.invoices.company.domain.entities;

import com.invoices.invoice.domain.entities.Company;
import com.invoices.user.domain.entities.User;

/**
 * Domain entity representing a user-company relationship.
 * Encapsulates the relationship between a user and a company, along with the user's role in that company.
 */
public class UserCompany {

    private UserCompanyId id;
    private String role;
    private Company company;  // Lazy loaded from repository
    private User user;        // Lazy loaded from repository

    public UserCompany() {
    }

    public UserCompany(UserCompanyId id, String role) {
        this.id = id;
        this.role = role;
    }

    public UserCompany(UserCompanyId id, String role, Company company, User user) {
        this.id = id;
        this.role = role;
        this.company = company;
        this.user = user;
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

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public String toString() {
        return "UserCompany{" +
                "id=" + id +
                ", role='" + role + '\'' +
                '}';
    }
}
