package com.invoices.company.domain.entities;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite ID for user-company relationships.
 * This is a domain entity that represents the relationship between a user and a company.
 */
public class UserCompanyId implements Serializable {

    private Long userId;
    private Long companyId;

    public UserCompanyId() {
    }

    public UserCompanyId(Long userId, Long companyId) {
        this.userId = userId;
        this.companyId = companyId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        UserCompanyId that = (UserCompanyId) o;
        return Objects.equals(userId, that.userId) && Objects.equals(companyId, that.companyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, companyId);
    }

    @Override
    public String toString() {
        return "UserCompanyId{" +
                "userId=" + userId +
                ", companyId=" + companyId +
                '}';
    }
}
