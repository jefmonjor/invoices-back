package com.invoices.company.infrastructure.persistence.repositories;

import com.invoices.company.infrastructure.persistence.entities.UserCompany;
import com.invoices.company.infrastructure.persistence.entities.UserCompanyId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserCompanyRepository extends JpaRepository<UserCompany, UserCompanyId> {
    List<UserCompany> findByIdUserId(Long userId);

    List<UserCompany> findByIdCompanyId(Long companyId);

    void deleteByIdCompanyId(Long companyId);

    long countByIdCompanyId(Long companyId);
}
